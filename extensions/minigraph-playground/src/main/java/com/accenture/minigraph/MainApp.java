/*

    Copyright 2018-2026 Accenture Technology

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 */

package com.accenture.minigraph;

import org.platformlambda.core.annotations.MainApplication;
import org.platformlambda.core.models.EntryPoint;
import org.platformlambda.core.system.AutoStart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

@MainApplication
public class MainApp implements EntryPoint {
    private static final Logger log = LoggerFactory.getLogger(MainApp.class);

    public static void main(String[] args) {
        AutoStart.main(args);
    }

    @Override
    public void start(String[] args) throws ExecutionException, InterruptedException {
        // suppress warning
        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
        log.info("Started");

//        var po = PostOffice.trackable("main.app", "101", "/js");
//
//        for (int i=0; i<10; i++) {
//            var statement = """
//                const result = () => {
//                    return {n} + {n};
//                }
//                var map = new Map();
//                map.set('a', result());
//                map.set('b', new Date().toISOString());
//                map.set('c', {n});
//                """;
//
//            var x = String.valueOf(i);
//            statement = statement.replace("{n}", x);
//            var result = po.request(new EventEnvelope().setTo("graph.js").setBody(Map.of("js", statement)), 5000);
//            log.info("{}", result.get().getBody());
//        }

    }
}
