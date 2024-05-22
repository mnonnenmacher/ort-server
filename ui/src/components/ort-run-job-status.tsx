/*
 * Copyright (C) 2024 The ORT Server Authors (See <https://github.com/eclipse-apoapsis/ort-server/blob/main/NOTICE>)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

import { getStatusBackgroundColor } from '@/helpers/get-status-colors';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import { org_eclipse_apoapsis_ortserver_api_v1_model_PagedResponse_org_eclipse_apoapsis_ortserver_api_v1_model_OrtRunSummary_ } from '@/api/requests';

type OrtRunJobStatusProps = {
  jobs: org_eclipse_apoapsis_ortserver_api_v1_model_PagedResponse_org_eclipse_apoapsis_ortserver_api_v1_model_OrtRunSummary_['data'][0]['jobs'];
};

export const OrtRunJobStatus = ({ jobs }: OrtRunJobStatusProps) => {
  return (
    <TooltipProvider>
      <div className="flex space-x-1">
        <Tooltip>
          <TooltipTrigger>
            <div
              className={`w-3 h-3 rounded-full ${getStatusBackgroundColor(jobs.analyzer?.status)}`}
            ></div>
          </TooltipTrigger>
          <TooltipContent>
            <span>
              Analyzer: {jobs.analyzer?.status || 'Not included in ORT Run'}
            </span>
          </TooltipContent>
        </Tooltip>
        <Tooltip>
          <TooltipTrigger>
            <div
              className={`w-3 h-3 rounded-full ${getStatusBackgroundColor(jobs.advisor?.status)}`}
            ></div>
          </TooltipTrigger>
          <TooltipContent>
            <span>
              Advisor: {jobs.advisor?.status || 'Not included in ORT Run'}
            </span>
          </TooltipContent>
        </Tooltip>
        <Tooltip>
          <TooltipTrigger>
            <div
              className={`w-3 h-3 rounded-full ${getStatusBackgroundColor(jobs.scanner?.status)}`}
            ></div>
          </TooltipTrigger>
          <TooltipContent>
            <span>
              Scanner: {jobs.scanner?.status || 'Not included in ORT Run'}
            </span>
          </TooltipContent>
        </Tooltip>
        <Tooltip>
          <TooltipTrigger>
            <div
              className={`w-3 h-3 rounded-full ${getStatusBackgroundColor(jobs.evaluator?.status)}`}
            ></div>
          </TooltipTrigger>
          <TooltipContent>
            <span>
              Evaluator: {jobs.evaluator?.status || 'Not included in ORT Run'}
            </span>
          </TooltipContent>
        </Tooltip>
        <Tooltip>
          <TooltipTrigger>
            <div
              className={`w-3 h-3 rounded-full ${getStatusBackgroundColor(jobs.reporter?.status)}`}
            ></div>
          </TooltipTrigger>
          <TooltipContent>
            <span>
              Reporter: {jobs.reporter?.status || 'Not included in ORT Run'}
            </span>
          </TooltipContent>
        </Tooltip>
      </div>
    </TooltipProvider>
  );
};