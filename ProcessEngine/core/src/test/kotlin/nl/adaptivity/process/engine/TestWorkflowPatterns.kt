/*
 * Copyright (c) 2016.
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

package nl.adaptivity.process.engine

import nl.adaptivity.process.engine.EngineTesting.EngineSpecBody
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.spek.describe
import nl.adaptivity.spek.given
import nl.adaptivity.spek.it
import nl.adaptivity.spek.xdescribe
import org.jetbrains.spek.api.Spek
import org.junit.jupiter.api.Assertions.assertEquals
import org.w3c.dom.Node
import java.security.Principal
import java.util.*

/**
 * Created by pdvrieze on 30/12/16.
 */
class TestWorkflowPatterns : Spek({

  givenEngine {

    beforeEachTest {
      stubMessageService.clear()
    }

    xdescribe("Control flow patterns") {

      describe("Basic control-flow patterns") {

        describe("WCP1: A sequential process") {
          testWCP1()
        }

        describe("WCP2: Parallel split") {
          testWCP2()
        }

        describe("WCP3: Synchronization / And join") {
          testWCP3()
        }

        describe("WCP4: XOR split") {
          testWCP4()
        }

        describe("WCP5: simple-merge") {
          testWCP5()
        }
      }

      describe("Advanced branching and synchronization patterns") {
        describe("WCP6: multi-choice / or-split") {
          given("ac1.condition=true, ac2.condition=false") {
            testWCP6(true, false)
          }
          given("ac1.condition=false, ac2.condition=true") {
            testWCP6(false, true)
          }
          given("ac1.condition=true, ac2.condition=true") {
            testWCP6(true, true)
          }

        }

        describe("WCP7: structured synchronized merge") {
          given("ac1.condition=true, ac2.condition=false") {
            testWCP7(true, false)
          }
          given("ac1.condition=false, ac2.condition=true") {
            testWCP7(false, true)
          }
          given("ac1.condition=true, ac2.condition=true") {
            testWCP7(true, true)
          }

        }

        xdescribe("WCP8: Multi-merge", "Multiple instantiations of a single node are not yet supported") {
          testWCP8()
        }

        describe("WCP9: Structured Discriminator") {
          testWCP9()
        }
      }

      describe("Structural patterns") {
        xdescribe("WCP10: arbitrary cycles", "Multiple instantiations of a single node are not yet supported") {

        }

        describe("WCP11: Implicit termination") {
          testWCP11()
        }
      }
    }

    describe("Abstract syntax patterns") {
      describe("WASP4: Vertical modularisation (subprocesses)") {
        testWASP4()

      }
    }
  }
})

private fun EngineSpecBody.testWCP1() {
  val model = object :Model("WCP1") {
    val start by startNode
    val ac1 by activity(start)
    val ac2 by activity(ac1)
    val end by endNode(ac2)
  }

  it("should have 4 children") {
    assertEquals(4, model.getModelNodes().size)
  }

  val validTraces = with(model) { trace{ start .. ac1 .. ac2 .. end } }

  val invalidTraces = with(model) {
    trace { ac1 or ac2 or end or (start .. ((ac1..end) or ac2)) }
  }

  testTraces(model, valid = validTraces, invalid = invalidTraces)
}

private fun EngineSpecBody.testWCP2() {
  val model = object : Model("WCP2") {
    val start by startNode
    val split by split(start) { min = 2; max = 2 }
    val ac1   by activity(split)
    val ac2   by activity(split)
    val end1  by endNode(ac1)
    val end2  by endNode(ac2)
  }
  val validTraces = with(model) { trace {
    start .. (
      (ac1 .. end1 .. ac2 ..(split % end2)) or
      (ac2 .. end2 .. ac1 .. (split % end1)))
  } }

  val invalidTraces = with(model) { trace {
    ac1 or ac2 or end1 or end2 or split or
    (start ..(split or
              end1 or
              end2 or
              ((ac1 or
               (ac1..end1)) .. (split or
                                end2)) or
              ((ac2 or
               (ac2..end1)) .. (split or
                                end1))
      )
    )
  } }

  testTraces(model, valid = validTraces, invalid = invalidTraces)
}

private fun EngineSpecBody.testWCP3() {
  val model = object: Model("WCP3") {
    val start by startNode
    val split by split(start) { min = 2; max = 2 }
    val ac1   by activity(split)
    val ac2   by activity(split)
    val join  by join(ac1, ac2){ min = 2; max = 2 }
    val end   by endNode(join)
  }
  val validTraces =  with(model) { trace {
    start .. (ac1 % ac2) .. (split % join % end)
  } }

  val invalidTraces = with(model) { trace {
    ac1 or ac2 or join or end or split or
    (start .. (split or end or join or
              (ac1 or
               ac2) .. (split or join or end)
              ))
  }}

  testTraces(model, valid = validTraces, invalid = invalidTraces)
}

private fun EngineSpecBody.testWCP4() {
  val model = object: Model("WCP4") {
    val start by startNode
    val split by split(start) { min = 1; max = 1 }
    val ac1 by activity(split)
    val ac2 by activity(split)
    val end1 by endNode(ac1)
    val end2 by endNode(ac2)
  }
  val validTraces = with(model) { trace {
    start .. ((ac1 .. (end1 % split)) or
              (ac2 .. (end2 % split)))
  } }

  val invalidTraces = with(model) { trace {
    ac1 or ac2 or end1 or end2 or split or
    (start .. (split or end1 or end2 or
               (ac1 .. (ac2 or end2)) or
               (ac2 .. (ac1 or end1))))
  } }

  testTraces(model, valid = validTraces, invalid = invalidTraces)
}

private fun EngineSpecBody.testWCP5() {
  val model = object: Model("WCP5") {
    val start by startNode
    val split by split(start) { min = 1; max = 1 }
    val ac1 by activity(split)
    val ac2 by activity(split)
    val join by join(ac1, ac2) { min = 1; max = 1 }
    val ac3 by activity(join )
    val end by endNode(ac3 )
  }
  val validTraces = with(model) { trace {
    start .. ((ac1 or ac2) .. (((split % join).. ac3 ..end)))
  } }
  val invalidTraces = with(model) { trace {
    ac1 or ac2 or ac3 or end or join or split or
      (start .. (ac3 or join or split or end or
//        ((ac1 or ac2) .. ac3) or, this passes as the system verify nonexistence of join/split/end nodes
        (ac1 % ac2)))
  } }

  testTraces(model, valid = validTraces, invalid = invalidTraces)
}

private fun EngineSpecBody.testWCP6(ac1Condition: Boolean,
                                    ac2Condition: Boolean) {
  val model = object : Model("WCP6") {
    val start by startNode
    val split by split(start) { min = 1; max = 2 }
    val ac1 by activity(split) { condition = ac1Condition.toXPath() }
    val ac2 by activity(split) { condition = ac2Condition.toXPath() }
    val end1 by endNode(ac1 )
    val end2 by endNode(ac2 )
  }
  val invalidTraces = mutableListOf<Trace>()
  val validTraces = with (model) { when {
    ac1Condition && ac2Condition -> {

      invalidTraces.addAll(trace {
        start .. ((ac1 .. end1.opt .. end2) or
          (ac2 .. end2.opt .. end1))
      })

      trace { start .. ( // these are valid
        (ac1 .. end1 .. ac2) or
        (ac2 .. end2 .. ac1)) .. (split % (end1 or end2))
      }.removeInvalid()
    }
    ac1Condition && !ac2Condition -> {
      invalidTraces.addAll(trace{
        start .. (end1 or ((ac1 .. end1.opt).opt .. (ac2 or end2)))
      })

      trace {
        start .. ac1 .. (split % end1)
      }

    }
    !ac1Condition && ac2Condition -> {
      invalidTraces.addAll(trace{
        start .. (end2 or ((ac2 .. end2.opt).opt .. (ac1 or end1)))
      })

      trace {
        start .. ac2 .. (split % end2)
      }

    }
    else -> kfail("All cases need valid traces")
  } }

  val baseInvalid = with(model) { trace {
    ac1 or ac2 or (start.opt .. (end1 or end2 or split))
  } }
  testTraces(model, valid = validTraces, invalid = baseInvalid + invalidTraces)
}

private fun EngineSpecBody.testWCP7(ac1Condition: Boolean,
                                    ac2Condition: Boolean) {
  val model = object: Model("WCP7") {
    val start by startNode
    val split by split(start) { min = 1; max = 2 }
    val ac1 by activity(split) { condition = ac1Condition.toXPath() }
    val ac2 by activity(split) { condition = ac2Condition.toXPath() }
    val join by join(ac1, ac2) {  min = 1; max=2 }
    val end by endNode(join)
  }

  val invalidTraces = mutableListOf<Trace>()
  val validTraces = with(model) { when {
    ac1Condition && ac2Condition -> {
      invalidTraces.addAll(trace {
        start .. (ac1 or ac2) .. (end or join or split)
      })

      trace {
        start .. (ac1 % ac2) .. (split % join % end)
      }
    }
    ac1Condition && !ac2Condition -> {
      invalidTraces.addAll(trace{
        start .. ac1.opt .. ac2
      })

      trace {
        start .. ac1 .. (join % split % end)
      }

    }
    !ac1Condition && ac2Condition -> {
      invalidTraces.addAll(trace{
        start .. ac2.opt .. ac1
      })


      trace {
        start .. ac2 .. (split % join % end)
      }
    }
    else -> kfail("All cases need valid traces")
  }}

  val baseInvalid = with(model) { trace {
    ac1 or ac2 or ( start.opt .. (end or join or split))
  }}
  testTraces(model, valid = validTraces, invalid = baseInvalid + invalidTraces)
}

private fun EngineSpecBody.testWCP8() {
  val model = object : Model("WCP8") {
    val start1 by startNode
    val start2 by startNode
    val ac1    by activity(start1)
    val ac2    by activity(start2)
    val join   by join(ac1, ac2) { min = 1; max = 1 }
    val ac3    by activity(join)
    val end    by endNode(ac3)
  }

  val validTraces = with(model) { trace {
    val t1 = ac3[1] .. end[1]
    val t2 = ac3[2] .. end[2]
    val h2 = (ac1 or ac2) .. join[2]

    (start1 % start2) .. (ac1 or ac2) .. join[1] ..
      ((t1 % h2).. t2) or (h2 .. t2 .. t1)
  }}.removeInvalid()

  val oldvalidTraces = listOf(
    trace(model.start1.id, "start2", "ac1", "join:1", "ac3:1", "end:1", "ac2", "join:2", "ac3:2", "end:2"),
    trace("start1", "start2", "ac1", "join:1", "ac2", "join:2", "ac3:1", "end:1", "ac3:2", "end:2"),
    trace("start1", "start2", "ac1", "join:1", "ac2", "join:2", "ac3:2", "end:2", "ac3:1", "end:1"),
    trace("start1", "start2", "ac2", "join:1", "ac3:1", "end:1", "ac1", "join:1", "ac3:2", "end:2"),
    trace("start1", "start2", "ac2", "join:1", "ac1", "join:2", "ac3:1", "end:1", "ac3:2", "end:2"),
    trace("start1", "start2", "ac2", "join:1", "ac1", "join:2", "ac3:2", "end:2", "ac3:1", "end:1"))

  val invalidTraces = with(model) { trace{
    ac1 or ac2 or ac3 or end or join or
      (((start1 % start2) or start1 or start2) .. (join or ac3 or end))
  }}

  val oldInvalidTraces = listOf("ac1", "ac2", "ac3", "end", "join").map { trace(it) } +
                         listOf("join", "ac3", "end").map { trace("start1", "start2", it) }
  testTraces(model, valid = validTraces, invalid = invalidTraces)
}

private fun EngineSpecBody.testWCP9() {
  val model = object : Model("WCP9") {
    val start1 by startNode
    val start2 by startNode
    val ac1 by activity(start1)
    val ac2 by activity(start2)
    val join by join(ac1, ac2){ min = 1; max = 1 }
    val ac3 by activity(join)
    val end by endNode(ac3)
  }
  val validTraces = with(model) { trace {
    (start1 % start2) .. (ac1 or ac2) .. join .. ac3 .. end
  }}
  val invalidTraces = with(model) { trace {
    val starts = (start1.opt % start2.opt).filter { it.elems.isNotEmpty() }
    ac1 or ac2 or (starts..(ac3 or end or join)) or
      (starts .. ac1 % ac2)
  }}

  testTraces(model, valid = validTraces, invalid = invalidTraces)
}

private fun EngineSpecBody.testWCP11() {
  val model = object: Model("WCP11") {
    val start1 by startNode
    val start2 by startNode
    val ac1    by activity(start1)
    val ac2    by activity(start2)
    val end1   by endNode(ac1)
    val end2   by endNode(ac2)
  }
  val validTraces = with(model) { trace {
    (start1 % start2) .. ((ac1 .. end1) % (ac2..end2))
  }}
  val invalidTraces = with(model) { trace {
    ac1 or ac2 or end1 or end2 or
      (((start1 % start2) or (start1 or start2))..(end1 or end2 or
        (ac1 .. end1.opt .. end2) or
        (ac2 .. end2.opt .. end1)))
    }
  }
  testTraces(model, valid = validTraces, invalid = invalidTraces)
}

private fun EngineSpecBody.testWASP4() {

  val model = object : Model("WASP4") {
    val start1 by startNode
    val ac1    by activity(start1)

    val comp1 by object : CompositeActivity(ac1) {
      val start2 by startNode
      val ac2    by activity(start2)
      val end2   by endNode(ac2)
    }
    val ac3    by activity(comp1)
    val end    by endNode(ac3)
  }
  val start2 = model.comp1.start2
  val ac2 = model.comp1.ac2
  val end2 = model.comp1.end2

  val validTraces = with(model) { trace{
    start1 .. ac1 .. start2 .. ac2 .. (end2 % comp1) .. ac3 ..end
  }}
  val invalidTraces = with(model) { trace {
    ac1 or comp1 or start2 or ac2 or end2 or ac3 or end or
      (start1 .. (comp1 or start2 or ac2 or end2 or ac3 or end or
        (ac1.. start2.opt .. (comp1 or end2 or ac3 or end or
          ( ac2 .. (comp1.opt % end2.opt) .. end )))))
  }}

  testTraces(model, valid = validTraces, invalid = invalidTraces)
}

internal fun Boolean.toXPath() = if (this) "true()" else "false()"

private inline fun <R> ProcessEngine<StubProcessTransaction>.testProcess(model: ExecutableProcessModel, owner: Principal, payload: Node? = null, body: (ProcessTransaction, ExecutableProcessModel, HProcessInstance) -> R):R {
  startTransaction().use { transaction ->

    val modelHandle = addProcessModel(transaction, model, owner)
    val instanceHandle = startProcess(transaction, owner, modelHandle, "testInstance", UUID.randomUUID(), payload)

    return body(transaction, transaction.readableEngineData.processModel(modelHandle).mustExist(modelHandle).withPermission(), instanceHandle)
  }
}

