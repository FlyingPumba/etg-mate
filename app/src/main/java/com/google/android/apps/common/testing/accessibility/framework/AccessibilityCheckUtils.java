/*
 * Copyright (C) 2014 Google Inc.
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

package com.google.android.apps.common.testing.accessibility.framework;

import android.os.Build;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import android.text.TextUtils;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;


import com.google.eyesfree.utils.AccessibilityNodeInfoUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for checking accessibility of apps.
 *
 * <p>All methods that take {@link View} or {@link AccessibilityNodeInfo} objects as arguments
 * require them to be fully initialized and in a valid hierarchy. Neither a newly constructed
 * {@link View} nor an object returned by {@code View.createAccessibilityNodeInfo()} have the
 * required properties because calls like {@code View.getRootView()} and
 * {@code AccessibilityNodeInfo.getParent} do not return reasonable values unless the objects are
 * part of a valid hierarchy.
 */
final class AccessibilityCheckUtils {

  private AccessibilityCheckUtils() {}

  /**
   * Retrieve text for a node, which may include text from children of the node. This text is an
   * approximation of, but not always identical to, what TalkBack would speak for the node. One
   * difference is that there are no separators between the speakable text from different nodes.
   *
   * @param info The info whose text should be returned.
   *
   * @return Speakable text derived from the info and its children. Returns an empty string if there
   *         is no such text, and {@code null} if {@code info == null}.
   */
  static CharSequence getSpeakableTextForInfo(AccessibilityNodeInfo info) {
    if (info == null) {
      return null;
    }

    /* getLabeledBy for API 17+ */
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {

        AccessibilityNodeInfo labeledBy = info.getLabeledBy();
        if (labeledBy != null) {
          /* There could be a chain of labeledBy. Make sure it isn't a loop */
          Set<AccessibilityNodeInfo> infosVisited = new HashSet<>();
          infosVisited.add(info);
          infosVisited.add(labeledBy);
          AccessibilityNodeInfo endOfLabeledByChain = labeledBy.getLabeledBy();
          while (endOfLabeledByChain != null) {
            if (infosVisited.contains(endOfLabeledByChain)) {
              infosVisited.remove(info);
              for (AccessibilityNodeInfo infoVisited : infosVisited) {
                infoVisited.recycle();
              }
              return null;
            }
            infosVisited.add(endOfLabeledByChain);
            labeledBy = endOfLabeledByChain;
            endOfLabeledByChain = labeledBy.getLabeledBy();
          }
          CharSequence labelText = getSpeakableTextForInfo(labeledBy);
          infosVisited.remove(info);
          for (AccessibilityNodeInfo infoVisited : infosVisited) {
            infoVisited.recycle();
          }
          return labelText;
        }
    }

    AccessibilityNodeInfoCompat compat = new AccessibilityNodeInfoCompat(info);

    // TODO(caseburkhardt) Pull in TalkBack's actual logic
    CharSequence nodeText = AccessibilityNodeInfoUtils.getNodeText(compat);
    StringBuilder returnStringBuilder = new StringBuilder((nodeText == null) ? "" : nodeText);
    /* If this node has a contentDescription, it overrides anything in children */
    if (TextUtils.isEmpty(compat.getContentDescription())) {
      for (int i = 0; i < compat.getChildCount(); ++i) {
        AccessibilityNodeInfoCompat child = compat.getChild(i);
        if (AccessibilityNodeInfoUtils.isVisibleOrLegacy(child)
            && !AccessibilityNodeInfoUtils.isActionableForAccessibility(child)) {
          returnStringBuilder.append(
              getSpeakableTextForInfo((AccessibilityNodeInfo) child.getInfo()));
        }
      }
    }
    return returnStringBuilder;
  }

}
