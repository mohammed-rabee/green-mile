package com.syriatel.d3m.greenmile

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.LocalTime

class TransformerTests {


    fun `should transform rec cdr to action`() {
        val rec = arrayOf(
                "20200101121000")

        val result = processRec(rec)

        assertEquals(result.timeStamp, LocalDateTime.of(2020, 1, 1, 12, 10, 0))
        assertEquals(result.type, ActionType.Call)



    }

    /*  @Test
      fun `should calculate total sms`() {
          val acc = listOf(1, 5, 8, 9)

          //sumOfSmsPerWeek(Action,acc)
          acc.forEach {
              assertEquals(it + 1, countOf(ActionType.Msg, it, Action(
                      type = ActionType.Msg
              )))
          }
          acc.forEach {
              assertEquals(it, countOf(ActionType.Msg, it, Action(
                      type = ActionType.Call
              )))
          }

          acc.forEach {
              assertEquals(it, countOf(ActionType.Msg, it, Action(
                      type = ActionType.ActivateBundle
              )))
          }


      }*/

    @Test
    fun `should calculate total actions`() {
        val acc = listOf(1, 5, 8, 9)

        //sumOfSmsPerWeek(Action,acc)
        acc.forEach { ac ->
            ActionType.values().forEach { at ->
                assertEquals(ac + 1, countOf(at, ac, Action(
                        type = at
                )))
                ActionType.values().toSet().filter { it != at }.forEach {
                    assertEquals(ac, countOf(at, ac, Action(
                            type = it
                    )))
                }
            }
        }

    }

    @Test
    fun `calculate sum of field based on criteria`() {

        val atNight: Action.() -> Boolean = {
            this.timeStamp.toLocalTime().let {
                it.isBefore(LocalTime.of(23, 59)) && it.isAfter(LocalTime.of(18, 0))
            }
        }


        val col: (String) -> (Action.() -> Number) = { name ->
            {
                getOrDefault(name, 0) as Number
            }
        }

        assertEquals(10.0, sumOf(8.0, Action(cost = 2.0, type = ActionType.Call, timeStamp = LocalDateTime.of(
                2020, 1, 1, 19, 10
        )), atNight, { this.cost }))


    }

    @Test
    fun `calculate sum of field based on criteria2`() {
        val actions = listOf(
                Action(performedBy = "0933886839", type = ActionType.Call, timeStamp = LocalDateTime.of(
                        2020, 1, 1, 12, 1)).apply { put("usageServiceType", 10) },

                Action(performedBy = "0933886850", type = ActionType.Call, timeStamp = LocalDateTime.of(
                        2020, 1, 1, 15, 10)).apply { put("usageServiceType", 10) },

                Action(performedBy = "0933886780", type = ActionType.Call, timeStamp = LocalDateTime.of(
                        2020, 1, 1, 19, 59)).apply { put("usageServiceType", 10) },

                Action(performedBy = "0933887850", type = ActionType.Call, timeStamp = LocalDateTime.of(
                        2020, 1, 1, 16, 10)).apply { put("usageServiceType", 47) },

                Action(performedBy = "0933789850", type = ActionType.Msg, timeStamp = LocalDateTime.of(
                        2020, 1, 1, 19, 10)).apply { put("usageServiceType", 10) },

                Action(performedBy = "0933789850", type = ActionType.Call, timeStamp = LocalDateTime.of(
                        2020, 1, 1, 21, 10)).apply { put("usageServiceType", 10) }
        )
        val results = listOf(1, 2, 3, 3, 3, 3)

        val criteria2: Action?.() -> Boolean = {
            if (this == null)
                false
            else
                (this.timeStamp.toLocalTime().let {
                    it.isAfter(LocalTime.MIDNIGHT) && it.isBefore(LocalTime.of(20, 0))
                }) && type == ActionType.Call && get("usageServiceType") == 10
        }




        actions.forEachIndexed { i, action ->
            assertEquals(results[i], count(if (i > 0) results[i - 1] else 0, action, criteria2))
        }


    }

    @Test
    fun `should find the latest action`(){
        val actions= listOf(
                Action(performedBy = "0933886839", type = ActionType.Call, timeStamp = LocalDateTime.of(
                        2020, 1, 1, 12, 1), cost=15.0),

                Action(performedBy = "0933886839", type = ActionType.Call, timeStamp = LocalDateTime.of(
                        2020, 1, 1, 15, 10),cost =16.0),

                Action(performedBy = "0933886839", type = ActionType.Call, timeStamp = LocalDateTime.of(
                        2020, 1, 1, 19, 59), cost =17.0),

                Action(performedBy = "0933886839", type = ActionType.Call, timeStamp = LocalDateTime.of(
                        2020, 1, 1, 16, 10) , cost = 19.0),

                Action(performedBy = "0933886839", type = ActionType.Msg, timeStamp = LocalDateTime.of(
                        2020, 1, 1, 19, 10), cost = 31.0),

                Action(performedBy = "0933886839", type = ActionType.Call, timeStamp = LocalDateTime.of(
                        2020, 1, 1, 21, 10), cost = 21.0)

        )
        fun newCriteria(action:Action):Boolean {
            return action.type == ActionType.Call
        }
        //Max(maxValue,action,criteria,field)
        var result:LocalDateTime?= null
        for( a in actions){
            result= max(result,a,{it.type == ActionType.Call})
                    { it.timeStamp }
        }
        assertEquals(LocalDateTime.of(2020, 1, 1, 21, 10),result)
/*
        var result2:Double= null

        for( a in actions){
            result2= max(result2,a,{it.type == ActionType.Call})
            { it.cost }
        }
        assertEquals(LocalDateTime.of(2020, 1, 1, 21, 10),result2)
*/


    }
}

/**
 * =========== Implement Functions Under
 */

fun <T:Comparable<T>> max(acc:T?,action:Action,criteria:(Action) -> Boolean,field: ((Action) -> T)? ): T? {
    if(field != null)
        if(acc == null)
            return field(action)
        else
            if(criteria(action) && field(action) > acc)
                return field(action)

    return acc
}
fun countOf(type: ActionType, it: Int, action: Action): Int {
    return if (action.type == type) it + 1
    else it
}


fun sumOf(acc: Number, action: Action, criteria: Action.() -> Boolean, field: (Action.() -> Number?)) =
        if (criteria(action)) {
            acc.toDouble() + (action.field()?.toDouble() ?: 0.0)
        } else acc.toDouble()


fun count(acc: Int, action: Action, fn: Action?.() -> Boolean = { this != null }): Int =
        sumOf(acc, action, fn, { 1 }).toInt()
