/*******************************************************************************
 *     Cloud Foundry 
 *     Copyright (c) [2009-2014] Pivotal Software, Inc. All Rights Reserved.
 *
 *     This product is licensed to you under the Apache License, Version 2.0 (the "License").
 *     You may not use this product except in compliance with the License.
 *
 *     This product includes a number of subcomponents with
 *     separate copyright notices and license terms. Your use of these
 *     subcomponents is subject to the terms and conditions of the
 *     subcomponent's license, as noted in the LICENSE file.
 *******************************************************************************/
package org.cloudfoundry.identity.uaa.login;

import org.cloudfoundry.identity.uaa.authentication.Origin;
import org.cloudfoundry.identity.uaa.oauth.approval.Approval;
import org.cloudfoundry.identity.uaa.oauth.approval.ApprovalsControllerService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoginUaaApprovalsService implements ApprovalsService {

    @Autowired
    private ApprovalsControllerService approvalsAdminEndpoints;

    @Override
    public Map<String, List<DescribedApproval>> getCurrentApprovalsByClientId() {
        Map<String, List<DescribedApproval>> result = new HashMap<>();
        List<Approval> approvalsResponse = approvalsAdminEndpoints.getApprovals("user_id pr", 1, 1000);

        List<DescribedApproval> approvals = new ArrayList<>();
        for (Approval approval : approvalsResponse) {
            DescribedApproval describedApproval = new DescribedApproval(approval);
            approvals.add(describedApproval);
        }

        for (DescribedApproval approval : approvals) {
            List<DescribedApproval> clientApprovals = result.get(approval.getClientId());
            if (clientApprovals == null) {
                clientApprovals = new ArrayList<>();
                result.put(approval.getClientId(), clientApprovals);
            }

            String scope = approval.getScope();
            if (!scope.contains(".")) {
                approval.setDescription("Access your data with scope '" + scope + "'");
                clientApprovals.add(approval);
            } else {
                String resource = scope.substring(0, scope.lastIndexOf("."));
                if (Origin.UAA.equals(resource)) {
                    // special case: don't need to prompt for internal uaa
                    // scopes
                    continue;
                }
                String access = scope.substring(scope.lastIndexOf(".") + 1);
                approval.setDescription("Access your '" + resource + "' resources with scope '" + access + "'");
                clientApprovals.add(approval);
            }
        }
        for (List<DescribedApproval> approvalList : result.values()) {
            Collections.sort(approvalList, new Comparator<DescribedApproval>() {
                @Override
                public int compare(DescribedApproval o1, DescribedApproval o2) {
                    return o1.getScope().compareTo(o2.getScope());
                }
            });
        }
        return result;
    }

    @Override
    public void updateApprovals(List<DescribedApproval> approvals) {
        approvalsAdminEndpoints.updateApprovals(approvals.toArray(new DescribedApproval[approvals.size()]));
    }

    @Override
    public void deleteApprovalsForClient(String clientId) {
        approvalsAdminEndpoints.revokeApprovals(clientId);
    }
}