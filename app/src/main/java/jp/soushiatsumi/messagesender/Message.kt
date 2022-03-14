/*
 * MIT License
 *
 * Copyright 2022 Soushi Atsumi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package jp.soushiatsumi.messagesender

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// @Entity(primaryKeys = [MessageColumnName.addressTo, MessageColumnName.addressCC, MessageColumnName.addressBCC, MessageColumnName.subject, MessageColumnName.body, "index"])
@Entity(indices = [Index(value = [MessageColumnName.addressTo, MessageColumnName.addressCC, MessageColumnName.addressBCC, MessageColumnName.subject, MessageColumnName.body], unique = true)])
data class Message(
    @ColumnInfo(name = MessageColumnName.addressTo) val addressTo: String,
    @ColumnInfo(name = MessageColumnName.addressCC) val addressCC: String,
    @ColumnInfo(name = MessageColumnName.addressBCC) val addressBCC: String,
    @ColumnInfo(name = MessageColumnName.subject) val subject: String,
    @ColumnInfo(name = MessageColumnName.body) val body: String,
    // A name of this column is "`index`"
    @PrimaryKey(autoGenerate = true) val index: Int,
)

class MessageColumnName {
    companion object {
        const val addressTo = "addressTo"
        const val addressCC = "addressCC"
        const val addressBCC = "addressBCC"
        const val subject = "subject"
        const val body = "body"
        const val index = "`index`"

        fun valueToName(value: String, context: Context): String {
            return when (value) {
                context.getString(R.string.none) -> index
                context.getString(R.string.address_To) -> addressTo
                context.getString(R.string.address_CC) -> addressCC
                context.getString(R.string.address_BCC) -> addressBCC
                context.getString(R.string.title) -> subject
                context.getString(R.string.body) -> body
                else -> throw Exception("${value::class.java.simpleName}($value) cannot be converted.")
            }
        }
    }
}
