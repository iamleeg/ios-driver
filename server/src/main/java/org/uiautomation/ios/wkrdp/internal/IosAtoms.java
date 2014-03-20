/*
 * Copyright 2011-2012 WebDriver committers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.uiautomation.ios.wkrdp.internal;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.StringWriter;

/**
 * The WebDriver atoms are used to ensure consistent behaviour cross-browser.
 */
public enum IosAtoms {

  BACK("back_ios.js"),
  CLEAR("clear_ios.js"),
  CLICK("click_ios.js"),
  FORWARD("forward_ios.js"),
  GET_ATTRIBUTE("get_attribute_ios.js"),
  GET_EFFECTIVE_STYLE("get_effective_style_ios.js"),
  GET_FRAMED_PAGE_OFFSET("getFramedPageOffset_ios.js"),
  GET_INTERACTABLE_SIZE("get_interactable_size_ios.js"),
  GET_LOCATION_IN_VIEW("getLocationInView_ios.js"),
  GET_PAGE_OFFSET("getPageOffset_ios.js"),
  GET_POSITION("getPageOffset_ios.js"),
  GET_VIEW_PORT_SIZE("getViewPortSize_ios.js"),
  GET_VISIBLE_TEXT("get_visible_text_ios.js"),
  IS_ENABLED("is_enabled_ios.js"),
  IS_SELECTED("is_selected_ios.js"),
  IS_SHOWN("is_shown_ios.js"),
  LINK_TEXT("link_text_ios.js"),
  LINK_TEXTS("link_texts_ios.js"),
  PARTIAL_LINK_TEXT("partial_link_text_ios.js"),
  PARTIAL_LINK_TEXTS("partial_link_texts_ios.js"),
  REFRESH("refresh_ios.js"),
  SCROLL_INTO_VIEW("scroll_into_view_ios.js"),
  SET_CURSOR_AT_THE_END("setCursorAtTheEnd_ios.js"),
  STRINGIFY("stringify_ios.js"),
  SUBMIT("submit_ios.js"),
  TYPE("type_ios.js"),
  XPATH("xpath_ios.js"),
  XPATHS("xpaths_ios.js"),
  GET_SIZE("get_element_size_ios.js");

  private final String value;

  public String getValue() {
    return value;
  }

  public String toString() {
    return getValue();
  }

  IosAtoms(String fileLocation) {
    StringWriter sw = new StringWriter();
    try {
      IOUtils
          .copy(this.getClass().getClassLoader().getResourceAsStream("atoms/" + fileLocation), sw);
    } catch (IOException e) {
      e.printStackTrace();
    }
    this.value = sw.toString();
  }

}
