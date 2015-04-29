/*
 *  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
// asset.manager = function(ctx) {
//  return {
//      get:function(id){
//          log.info('overridden get method in the GREG default extension');
//          return this._super.get.call(this,id);
//      }
//  }
// };
asset.renderer = function(ctx) {
    var gregAPI = require('/modules/greg-publisher-api.js').gregAPI;
    var rxt = require('rxt');
    var assetManager = function(session, type) {
        var am = rxt.asset.createUserAssetManager(session, type);
        return am;
    };
    var getAssetCommentManager = function (ctx) {
        var rxt = require('rxt');
        var am = rxt.asset.createUserAssetManager(ctx.session, 'comments');
        return am;
    };
    return {
        pageDecorators: {
            sidebarPopulator: function(page) {
                if (page.meta.pageName === 'details') {
                    page.isSidebarEnabled = true;
                }
            },
            subscriptionPopulator: function(page) {
                if (page.meta.pageName === 'details') {
                    var am = assetManager(ctx.session,ctx.assetType);
                    log.info('### obtaining subscriptions ###');
                    page.subscriptions = gregAPI.subscriptions.list(am,page.assets.id);
                    log.info('### done ###');
                }
            },
            notificationPopulator: function(page) {
                if (page.meta.pageName === 'details') {
                    page.notificationsCount = gregAPI.notifications.count();
                }
            },
            notificationListPopulator: function(page) {
                if (page.meta.pageName === 'details') {
                    var am = assetManager(ctx.session,ctx.assetType);
                    page.notifications = gregAPI.notifications.list(am);
                }
            },
            notePopulator: function(page) {
                if (page.meta.pageName === 'details') {}
            },
            comments: function (page) {
                if (!page.assets.id) {
                    return;
                }
                var q = {};
                q.overview_resourcepath = page.assets.path;
                var items = getAssetCommentManager(ctx).search(q);
                page.comments = items;
                log.info(page.comments);
            }
        }
    };
};
asset.configure = function() {
    return {
        meta: {
            lifecycle: {
                commentRequired: false,
                defaultAction: '',
                deletableStates: [],
                defaultLifecycleEnabled: false,
                publishedStates: ['Published']
            },
            grouping: {
                groupingEnabled: false,
                groupingAttributes: ['overview_name']
            }
        }
    };
};
// asset.configure = function(){
//  return {
//      meta:{
//          lifecycle:{
//              lifecycleViewEnabled:false,
//              lifecycleEnabled:false
//          },
//          grouping:{
//              groupingEnabled:true
//          }
//      }
//  };
// };