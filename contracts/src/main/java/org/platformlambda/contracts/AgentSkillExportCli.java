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

package org.platformlambda.contracts;

import java.io.PrintStream;
import java.nio.file.Path;

/** One-shot, local operator entrypoint for exporting the installed Mercury skill snapshot. */
public final class AgentSkillExportCli {
    private AgentSkillExportCli() {
        // utility class
    }

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        if (args == null || args.length != 1 || args[0] == null || args[0].isBlank()) {
            err.println(ContractError.INVALID_EXPORT_ROOT.name() + ": "
                    + ContractError.INVALID_EXPORT_ROOT.message());
            return 2;
        }
        try {
            new AgentSkillExporter(ContractRegistry.load()).export(Path.of(args[0]));
            out.println("Mercury platform skill exported");
            return 0;
        } catch (ContractException e) {
            err.println(e.getError().name() + ": " + e.getError().message());
            if (e.getSuppressed().length > 0) {
                err.println(ContractError.EXPORT_CLEANUP_FAILED.name() + ": "
                        + ContractError.EXPORT_CLEANUP_FAILED.message());
            }
            return 1;
        } catch (RuntimeException e) {
            err.println(ContractError.EXPORT_IO_FAILED.name() + ": "
                    + ContractError.EXPORT_IO_FAILED.message());
            return 1;
        }
    }
}
