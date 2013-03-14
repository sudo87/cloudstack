// Copyright 2012 Citrix Systems, Inc. Licensed under the
// Apache License, Version 2.0 (the "License"); you may not use this
// file except in compliance with the License.  Citrix Systems, Inc.
// reserves all rights not expressly granted by the License.
// You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

(function($, cloudStack) {

  cloudStack.uiCustom.healthCheck = function(args) {

    // Place outer args here as local variables
    // i.e, -- var dataProvider = args.dataProvider

    return function(args){         		
			if(args.context.multiRules == undefined) { //LB rule is not created yet
			  cloudStack.dialog.notice({ message: _l('Health Check can only be configured on a created LB rule') });
				return;
			}
			
      var formData = args.formData;
      var forms = $.extend(true, {}, args.forms);
      var topFieldForm, bottomFieldForm , $topFieldForm , $bottomFieldForm;
      var topfields = forms.topFields; 

      var $healthCheckDesc = $('<div>Your load balancer will automatically perform health checks on your cloudstack instances and only route traffic to instances that pass the health check </div>').addClass('health-check-description');
      var $healthCheckConfigTitle = $('<div><br><br>Configuration Options :</div>').addClass('health-check-config-title');
      var $healthCheckAdvancedTitle = $('<div><br><br> Advanced Options : </div>').addClass('health-check-advanced-title');
    
      var $healthCheckDialog = $('<div>').addClass('health-check');
      $healthCheckDialog.append($healthCheckDesc);
      $healthCheckDialog.append($healthCheckConfigTitle);
    	var $loadingOnDialog = $('<div>').addClass('loading-overlay');
							
      var policyObj = null;				
			var pingpath1 = '/';
			var responsetimeout1 = '2';
			var healthinterval1 = '5';			
			var healthythreshold1 = '2';
			var unhealthythreshold1 = '1';
			
			$.ajax({
			  url: createURL('listLBHealthCheckPolicies'),
				data: {
				  lbruleid: args.context.multiRules[0].id
				},
				async: false,
				success: function(json) {		
					if(json.listlbhealtcheckpoliciesresponse.healthcheckpolicies[0].healthcheckpolicy[0] !=	undefined) {
					  policyObj = json.listlbhealtcheckpoliciesresponse.healthcheckpolicies[0].healthcheckpolicy[0];
						pingpath1 = policyObj.pingpath; //API bug: API doesn't return it
						responsetimeout1 = policyObj.responsetime;
						healthinterval1 =	policyObj.healthcheckinterval;		
						healthythreshold1 = policyObj.healthcheckthresshold;
						unhealthythreshold1 =	policyObj.unhealthcheckthresshold;
					}
				}
			});
     
      topFieldForm = cloudStack.dialog.createForm({
          context: args.context,
          noDialog: true, // Don't render a dialog, just return $formContainer
          form: {
            title: '',
            fields:{               					 
               pingpath: {label: 'Ping Path', docID:'helpAccountUsername' , validation: {required: false}, defaultValue: pingpath1}
             }
          }
        });

      $topFieldForm = topFieldForm.$formContainer;
      $topFieldForm.appendTo($healthCheckDialog);

      $healthCheckDialog.append($healthCheckAdvancedTitle);

      bottomFieldForm = cloudStack.dialog.createForm ({
        context:args.context,
        noDialog:true,
        form:{
          title:'',
          fields:{
            responsetimeout: {label: 'Response Timeout (in sec)' , validation:{required:false}, defaultValue: responsetimeout1},
            healthinterval: {label: 'Health Check Interval (in sec)',  validation:{required:false}, defaultValue: healthinterval1},                    
            healthythreshold:  {label: 'Healthy Threshold', validation: {required:false}, defaultValue: healthythreshold1},
					  unhealthythreshold: {label: 'Unhealthy Threshold' , validation: { required:false}, defaultValue: unhealthythreshold1}
          }
        }
      });

      $bottomFieldForm = bottomFieldForm.$formContainer;
      $bottomFieldForm.appendTo($healthCheckDialog);
					
					
			var buttons = [
				{
					text: _l('label.cancel'),
					'class': 'cancel',
					click: function() {
						$healthCheckDialog.dialog('destroy');
						 $('.overlay').remove();
					}
				}
			];	
			
			if(policyObj == null) { //policy is not created yet
			  buttons.push(				  
          {
            text: _l('Create'),
            'class': 'ok',
            click: function() {     
              $loadingOnDialog.appendTo($healthCheckDialog);							
						  var formData = cloudStack.serializeForm($healthCheckDialog.find('form'));							
							var data = {
							        lbruleid: args.context.multiRules[0].id, 					
								pingpath: formData.pingpath,
								responsetimeout: formData.responsetimeout,
								intervaltime: formData.healthinterval,
								healthythreshold: formData.healthythreshold,
								unhealthythreshold: formData.unhealthythreshold								
							};

                                                         var lbRuleData = {

                                                           algorithm:args.context.multiRules[0].algorithm,
                                                           name:args.context.multiRules[0].name,
                                                           publicport:args.context.multiRules[0].publicport,
                                                           privateport:args.context.multiRules[0].privateport


                                                            }

                                                        if(args.context.multiRules[0] != null)
                                                           $.extend(data , lbRuleData);
														
							$.ajax({
							  url: createURL('createLBHealthCheckPolicy'),
								data: data,
								success: function(json) {								  
									var jobId = json.createlbhealthcheckpolicyresponse.jobid;
									var createLBHealthCheckPolicyIntervalId = setInterval(function(){									  
										$.ajax({
										  url: createURL('queryAsyncJobResult'),
											data: {
											  jobid: jobId
											},
											success: function(json) {	
												var result = json.queryasyncjobresultresponse;
												if (result.jobstatus == 0) {
													return; //Job has not completed
												}
												else {                                      
													clearInterval(createLBHealthCheckPolicyIntervalId); 
													
													if (result.jobstatus == 1) {	
													  cloudStack.dialog.notice({ message: _l('Health Check Policy has been created') });														
                            $loadingOnDialog.remove();	
														$healthCheckDialog.dialog('destroy');
                            $('.overlay').remove();
													}
													else if (result.jobstatus == 2) {		
													  cloudStack.dialog.notice({ message: _s(result.jobresult.errortext) });			
                            $loadingOnDialog.remove();	
														$healthCheckDialog.dialog('destroy');
                            $('.overlay').remove();		
													}
												}																						
											}
										});										
									}, g_queryAsyncJobResultInterval); 										
								},
                                                              error:function(XMLHttpResponse){
                                                                   args.response.error(parseXMLHttpResponse(XMLHttpResponse));


                                                                }
							});
            }
          }					
				);
			}
			else { //policy exists already			  
				buttons.push(	
          //Update Button (begin) - call delete API first, then create API				
          {
            text: _l('Update'),
            'class': 'ok',
            click: function() {    
              $loadingOnDialog.appendTo($healthCheckDialog);
						
							$.ajax({
							  url: createURL('deleteLBHealthCheckPolicy'),
								data: {
								  id : policyObj.id
								},
								success: function(json) {								  						
									var jobId = json.deletelbhealthcheckpolicyresponse.jobid;
									var deleteLBHealthCheckPolicyIntervalId = setInterval(function(){									  
										$.ajax({
										  url: createURL('queryAsyncJobResult'),
											data: {
											  jobid: jobId
											},
											success: function(json) {	
												var result = json.queryasyncjobresultresponse;
												if (result.jobstatus == 0) {
													return; //Job has not completed
												}
												else {                                      
													clearInterval(deleteLBHealthCheckPolicyIntervalId); 
													
													if (result.jobstatus == 1) {	
														var formData = cloudStack.serializeForm($healthCheckDialog.find('form'));	                           										
														var data = {
															lbruleid: args.context.multiRules[0].id,					
															pingpath: formData.pingpath,
															responsetimeout: formData.responsetimeout,
															intervaltime: formData.healthinterval,
															healthythreshold: formData.healthythreshold,
															unhealthythreshold: formData.unhealthythreshold								
														};

                                                                                                                 var lbRuleData = {

                  algorithm:args.context.multiRules[0].algorithm,
                  name:args.context.multiRules[0].name,
                  publicport:args.context.multiRules[0].publicport,
                  privateport:args.context.multiRules[0].privateport


              }

           if(args.context.multiRules[0] != null)
            $.extend(data , lbRuleData);
																					
														$.ajax({
															url: createURL('createLBHealthCheckPolicy'),
															data: data,
															success: function(json) {								  
																var jobId = json.createlbhealthcheckpolicyresponse.jobid;
																var createLBHealthCheckPolicyIntervalId = setInterval(function(){									  
																	$.ajax({
																		url: createURL('queryAsyncJobResult'),
																		data: {
																			jobid: jobId
																		},
																		success: function(json) {	
																			var result = json.queryasyncjobresultresponse;
																			if (result.jobstatus == 0) {
																				return; //Job has not completed
																			}
																			else {                                      
																				clearInterval(createLBHealthCheckPolicyIntervalId); 
																				
																				if (result.jobstatus == 1) {	   
																					cloudStack.dialog.notice({ message: _l('Health Check Policy has been updated') });
																					$loadingOnDialog.remove();	
																					$healthCheckDialog.dialog('destroy');
																					$('.overlay').remove();																					
																				}
																				else if (result.jobstatus == 2) {																				  
																					cloudStack.dialog.notice({ message: _s(result.jobresult.errortext) });		
                                          $loadingOnDialog.remove();	
																					$healthCheckDialog.dialog('destroy');
																					$('.overlay').remove();																										
																				}
																			}																						
																		}
																	});										
																}, g_queryAsyncJobResultInterval); 										
															},
                                                                                                                        error:function(json){
 															   args.response.error(parseXMLHttpResponse(json));

                                                                                                                                }
														});														
													}
													else if (result.jobstatus == 2) {													  
														cloudStack.dialog.notice({ message: _s(result.jobresult.errortext) });	
                            $loadingOnDialog.remove();	
														$healthCheckDialog.dialog('destroy');
														$('.overlay').remove();																
													}
												}																						
											}
										});										
									}, g_queryAsyncJobResultInterval); 										
								}
							});												
            }
          }
					//Update Button (end) 
				);				
			}
						
      $healthCheckDialog.dialog({
        title: 'Health Check Wizard',
        width: 600,
        height: 600,
        draggable: true,
        closeonEscape: false,
        overflow:'auto',
        open:function() {
          $("button").each(function(){
            $(this).attr("style", "left: 400px; position: relative; margin-right: 5px; ");
          });
        },
        buttons: buttons
      }).closest('.ui-dialog').overlay();

    }
  }
 }(jQuery, cloudStack));


