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

package nl.adaptivity.process.util

import kotlinx.serialization.*
import nl.adaptivity.xmlutil.serialization.DelegateSerializer
import nl.adaptivity.xmlutil.serialization.simpleSerialClassDesc

/**
 * Interface for objects that may have identifiers.
 */
interface Identifiable : Comparable<Identifiable> {

    val id: String?

    @Transient
    val identifier: Identifier?
        get() = id?.let(::Identifier)

    override fun compareTo(other: Identifiable): Int {
        val otherId = other.id
        return if (otherId == null) {
            id?.let { 1 } ?: 0
        } else {
            id?.compareTo(otherId) ?: -1
        }
    }

    @Serializer(forClass = Identifiable::class)
    companion object {
        override val serialClassDesc: KSerialClassDesc
            get() = simpleSerialClassDesc<Identifiable>()

        override fun load(input: KInput): Identifiable {
            return Identifier(input.readStringValue())
        }

        override fun save(output: KOutput, obj: Identifiable) {
            val value = obj.id
            if (value == null) {
                output.writeNullValue()
            } else {
                output.writeNotNullMark()
                output.writeStringValue(value)
            }
        }
    }

}

object IdentifiableListSerializer: DelegateSerializer<List<Identifiable>>(Identifiable.list)

object IdentifiableSetSerializer: DelegateSerializer<Set<Identifiable>>(Identifiable.set)
