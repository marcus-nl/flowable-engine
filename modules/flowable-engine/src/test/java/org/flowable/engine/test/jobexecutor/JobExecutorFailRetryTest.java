/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.engine.test.jobexecutor;

import static org.assertj.core.api.Assertions.*;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;

/**
 * @author Saeid Mirzaei
 */
public class JobExecutorFailRetryTest extends PluggableFlowableTestCase {

    @Deployment
    public void testFailedServiceTask() {
        // process throws no exception. Service task passes at the first time.
        RetryFailingDelegate.initialize(0);
        ProcessInstance instance1 = runtimeService.startProcessInstanceByKey("failedJobRetry");

        waitForJobExecutorToProcessAllJobs(1000, 200);
        assertThat(RetryFailingDelegate.getNumCalls()).isEqualTo(1); // check number of calls of delegate
        
        assertProcessEnded(instance1.getId());

        // process throws exception 2 times, with 3 seconds in between
        RetryFailingDelegate.initialize(2); // throw exception 2 times
        ProcessInstance instance2 = runtimeService.startProcessInstanceByKey("failedJobRetry");

        executeJobExecutorForTime(9000, 500);
        assertThat(RetryFailingDelegate.getNumCalls()).isEqualTo(3);
        assertThat(RetryFailingDelegate.getTimeDiff()).isBetween(3000L, 5000L); // check time difference between last 2 calls. Just roughly
        
        assertProcessEnded(instance2.getId());

        // process throws exception 3 times, with 3 seconds in between
        RetryFailingDelegate.initialize(3); // throw exception 3 times
        ProcessInstance instance3 = runtimeService.startProcessInstanceByKey("failedJobRetry");

        executeJobExecutorForTime(9000, 500);
        assertThat(RetryFailingDelegate.getNumCalls()).isEqualTo(3);
        assertThat(RetryFailingDelegate.getTimeDiff()).isBetween(3000L, 5000L);
        
        // since there are 3 retries, which all fail, the process should NOT be complete.
        assertEquals(1, processEngine.getRuntimeService().createProcessInstanceQuery().processInstanceId(instance3.getId()).count());
    }
}
