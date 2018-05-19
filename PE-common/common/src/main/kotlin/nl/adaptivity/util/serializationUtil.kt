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

package nl.adaptivity.util

import kotlinx.serialization.KSerialClassDesc
import kotlinx.serialization.internal.SerialClassDescImpl

fun SerialClassDescImpl(original: KSerialClassDesc, name: String): SerialClassDescImpl {
    return SerialClassDescImpl(name).apply {
        for(i in 0 until original.associatedFieldsCount) {
            addElement(original.getElementName(i))
            for(a in original.getAnnotationsForIndex(i)) {
                pushAnnotation(a)
            }
        }
    }
}

fun KSerialClassDesc.describe(): String {
    return (0 until associatedFieldsCount).joinToString(",\n", prefix="$name[$kind] (", postfix = ")") {
        getAnnotationsForIndex(it).joinToString(postfix = " : ${getElementName(it)}")
    }
}