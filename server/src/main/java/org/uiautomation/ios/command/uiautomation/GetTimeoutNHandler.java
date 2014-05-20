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

package org.uiautomation.ios.command.uiautomation;

import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.Response;
import org.uiautomation.ios.communication.WebDriverLikeRequest;
import org.uiautomation.ios.IOSServerManager;
import org.uiautomation.ios.command.PostHandleDecorator;
import org.uiautomation.ios.command.UIAScriptHandler;
import org.uiautomation.ios.utils.JSTemplate;
import org.uiautomation.ios.utils.hack.TimeSpeeder;

public class GetTimeoutNHandler extends UIAScriptHandler {

  private static final JSTemplate template = new JSTemplate(
      "var timeout = UIAutomation.getTimeout('%:type$s');" +
      "UIAutomation.createJSONResponse('%:sessionId$s',0,timeout)",
      "sessionId", "type");

  public GetTimeoutNHandler(IOSServerManager driver, WebDriverLikeRequest request)
      throws Exception {
    super(driver, request);
    String type = request.getPayload().getString("type");
    setJS(template.generate(request.getSession(), type));
    addDecorator(new CorrectTimeout(driver));
  }

  class CorrectTimeout extends PostHandleDecorator {

    public CorrectTimeout(IOSServerManager driver) {
      super(driver);
    }

    @Override
    public void decorate(Response response) {
      try {
        Integer timeout = (Integer) response.getValue();
        float timeCorrection = TimeSpeeder.getInstance().getSecondDuration();
        float correctTimeout = timeout / timeCorrection;
        response.setValue((int) correctTimeout);
      } catch (Exception e) {
        throw new WebDriverException(
            "error correcting the timeout to take the timespeeder into account." + e.getMessage(),
            e);
      }
    }
  }

  @Override
  public JSONObject configurationDescription() throws JSONException {
    return noConfigDefined();
  }
}