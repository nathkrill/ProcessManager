/*
 * Copyright (c) 2018.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.engine.patterns

import nl.adaptivity.process.engine.ConfigurableModel
import nl.adaptivity.process.engine.ModelData
import nl.adaptivity.process.engine.ModelSpek
import nl.adaptivity.process.engine.trace

/**
 * Test based on
 * https://bpmai.org/foswiki/pub/BPMAcademicInitiative/AnalyzeProcessModels/ex1_execution_traces.pdf
 */
class WebProcess1 : ModelSpek(run {
    val m = object : ConfigurableModel("Signavio-insurance-emergency") {
        val start by startNode { label = "Insurance emergency" }

        val split1 by split(start) {
            min=2
            max=2
        }

        val ac1 by activity(split1) {
            label="Analyze insurance agreement"
        }

        val split2 by split(ac1) {
            min=1
            max=1
        }

        val ac2 by activity(split1) {
            label="Offer immediate help"
        }

        val split3 by split(ac2) {
            min=1
            max=1
        }

        val join1 by join(split2, split3) {
            conditions[split2] = "coverate exists"
            conditions[split3] = "accepted"
            min=2
            max=2
        }

        val ac3 by activity(split2) {
            condition="no coverage"
            label = "Send out offer for emergency help"
        }

        val ac4 by activity(join1) {
            label = "Do internal accounting"
        }

        val ac5 by activity(split2) {
            label = "Ask for rejection notification"
        }

        val join2 by join(ac4, ac5) {
            min=1
            max=1
        }

        val join3 by join(ac3, join2) {
            min=2
            max=2
        }

        val end by endNode(join3)
    }
    with(m) {
        val valid = trace { start..split1..ac1..split2..ac2..split3..ac3..ac5..end }
        val invalid = trace {
            (start.opt * (ac2 or end)) or
                (start..ac1..end)
        }
        ModelData(m, valid, invalid)
    }
}, null)