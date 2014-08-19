/*
 * Copyright 2012-2013 eBay Software Foundation and ios-driver committers
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.uiautomation.ios.instruments;

import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.Response;
import org.uiautomation.ios.Device;
import org.uiautomation.ios.IOSCapabilities;
import org.uiautomation.ios.RealDevice;
import org.uiautomation.ios.ServerSideSession;
import org.uiautomation.ios.application.IOSRunningApplication;
import org.uiautomation.ios.command.UIAScriptRequest;
import org.uiautomation.ios.command.UIAScriptResponse;
import org.uiautomation.ios.instruments.commandExecutor.CURLIAutomationCommandExecutor;
import org.uiautomation.ios.instruments.commandExecutor.UIAutomationCommandExecutor;
import org.uiautomation.ios.utils.AppleMagicString;
import org.uiautomation.ios.utils.ApplicationCrashListener;
import org.uiautomation.ios.utils.ClassicCommands;
import org.uiautomation.ios.utils.Command;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.uiautomation.ios.instruments.commandExecutor.CommunicationMode.CURL;

public class InstrumentsCommandLine implements Instruments {

  private static final Logger log = Logger.getLogger(InstrumentsCommandLine.class.getName());
  private final String uuid;
  private final File template;
  private final IOSRunningApplication application;
  private final File output;
  private final String sessionId;
  private final List<String> envtParams;
  private final Command instruments;
  private final CURLIAutomationCommandExecutor channel;
  private final InstrumentsVersion version;
  private final TakeScreenshotService screenshotService;
  private final IOSCapabilities caps;
  private final String desiredSDKVersion;
  private final ServerSideSession session;
  private long instrumentsPid = -1;

  public InstrumentsCommandLine(ServerSideSession session) {
    Device device = session.getDevice();
    if (device instanceof RealDevice) {
      uuid = ((RealDevice) device).getUuid();
    } else {
      uuid = null;
    }
    this.caps = session.getCapabilities();
    this.version = session.getIOSServerManager().getHostInfo().getInstrumentsVersion();
    this.sessionId = session.getSessionId();
    this.application = session.getApplication();
    this.envtParams = caps.getExtraSwitches();
    this.session = session;
    this.desiredSDKVersion = caps.getSDKVersion();
    template = ClassicCommands.getAutomationTemplate();

    String appPath = application.getDotAppAbsolutePath();

    int port = session.getOptions().getPort();
    File
        scriptPath =
        new ScriptHelper().getScript(port, appPath, sessionId, CURL, caps.isAcceptAllCerts());
    output = createTmpOutputFolder();

    instruments = createInstrumentCommand(scriptPath);
    instruments.registerListener(new ApplicationCrashListener(session));
    instruments.setWorkingDirectory(output);

    channel = new CURLIAutomationCommandExecutor(sessionId);

    screenshotService = new InstrumentsAppleScreenshotService(this, sessionId);


  }

  @Override
  public void start(long timeout) throws InstrumentsFailedToStartException {
    boolean success = false;
    try {
      instruments.start();
      // for the no delay instruments, the command launches a script that in turn launches instruments.
      // need to keep the pid of intruments itself to be able to kill it.

      // let the process spawn
      Thread.sleep(2000);
      instrumentsPid = ClassicCommands.getHighestPidForName("instruments");

      log.fine("waiting for registration request");
      log.warning("starting to wait");
      success = channel.waitForUIScriptToBeStarted(timeout);
      log.fine("registration request received" + session.getCachedCapabilityResponse());
      if (!success) {
        log.warning("instruments crashed (" + timeout + " sec)".toUpperCase());
        throw new InstrumentsFailedToStartException(
            "Didn't get the capability back.Most likely, instruments crashed at startup.");
      }
    } catch (InterruptedException e) {
      throw new InstrumentsFailedToStartException("instruments was interrupted while starting.");
    } finally {
      // appears only in ios6. : Automation Instrument ran into an exception
      // while trying to run the script. UIAScriptAgentSignaledException
      if (!success) {
        instruments.forceStop();
      }
    }
  }

  @Override
  public void stop() {

    if(session.getDevice() instanceof RealDevice){
      try {
        Thread.sleep(1500);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    instruments.forceStop();
    try {
      ClassicCommands.kill(instrumentsPid);
    } catch (Exception e) {
      if (!session.hasCrashed()) {
        log.warning("couldn't kill " + instrumentsPid);
      }
    }
    channel.stop();
  }

  private Command createInstrumentCommand(File script) {
    List<String> args = new ArrayList<>();

    args.add(getInstrumentsClient());
    args.add("-v");
    if (uuid != null) {
      args.add("-w");
      args.add(uuid);
    } else if (application.isSimulator() && Double.parseDouble(version.getBuild()) >= 55044) {
      // newer instruments require to specify the simulator SDK and device type
      args.add("-w");
      args.add(AppleMagicString.getDeviceSpecification(caps.getDevice(), caps.getDeviceVariation(),
                                                       desiredSDKVersion, version));
    }
    args.add("-t");
    args.add(template.getAbsolutePath());
    args.add(application.getDotAppAbsolutePath());
    args.add("-e");
    args.add("UIASCRIPT");
    args.add(script.getAbsolutePath());
    args.add("-e");
    args.add("UIARESULTSPATH");
    args.add(output.getAbsolutePath());
    args.addAll(envtParams);
    return new Command(args, false);
  }

  private File createTmpOutputFolder() {
    try {
      File output = File.createTempFile(sessionId, null);
      output.delete();
      output.mkdir();
      output.deleteOnExit();
      return output;
    } catch (IOException e) {
      throw new WebDriverException(
          "Cannot create the tmp folder where all the instruments tmp files"
          + "will be stored.", e);
    }
  }

  private String getInstrumentsClient() {
    return InstrumentsNoDelayLoader.getInstance(version).getInstruments().getAbsolutePath();
  }

  @Override
  public Response executeCommand(UIAScriptRequest request) {
    UIAScriptResponse res = channel.executeCommand(request);
    return res.getResponse();
  }

  @Override
  public UIAutomationCommandExecutor getChannel() {
    return channel;
  }

  @Override
  public TakeScreenshotService getScreenshotService() {
    return screenshotService;
  }

  @Override
  public boolean isCompatible(ServerSideSession session) {
    if (session.isSafariRealDevice()) {
      return false;
    } else {
      return true;
    }
  }


  public File getOutput() {
    return output;
  }


}
