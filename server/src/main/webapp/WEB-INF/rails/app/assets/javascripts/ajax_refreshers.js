/*
 * Copyright Thoughtworks, Inc.
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
var AjaxRefreshers = function () {
  var ajaxRefreshers = [];
  var mainContentRefresher = {
    afterRefreshOf: function(_, executeThis) {
      executeThis();
    }
  };

  return {
    disableAjax: function() {
      ajaxRefreshers.forEach(function (ajaxRefresher) {
        ajaxRefresher.stopRefresh();
      });
    },

    enableAjax: function() {
      ajaxRefreshers.forEach(function (ajaxRefresher) {
        ajaxRefresher.restartRefresh();
      });
    },

    main: function() {
      return mainContentRefresher;
    },

    addRefresher: function(refresher, isMainContentRefresher) {
      if (isMainContentRefresher) {
        mainContentRefresher = refresher;
      }
      ajaxRefreshers.push(refresher);
    },

    clear: function() {
      mainContentRefresher = {
        afterRefreshOf: function(_, executeThis) {
          executeThis();
        }
      };
      ajaxRefreshers.length = 0;
    }
  };
}();

