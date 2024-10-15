/*
 * MIT License
 *
 * Copyright 2024 Soushi Atsumi
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
@Entity(indices = [Index(value = [MessageColumnName.ADDRESS_TO, MessageColumnName.ADDRESS_CC, MessageColumnName.ADDRESS_BCC, MessageColumnName.SUBJECT, MessageColumnName.BODY], unique = true)])
data class Message(
    @ColumnInfo(name = MessageColumnName.ADDRESS_TO) val addressTo: String,
    @ColumnInfo(name = MessageColumnName.ADDRESS_CC) val addressCC: String,
    @ColumnInfo(name = MessageColumnName.ADDRESS_BCC) val addressBCC: String,
    @ColumnInfo(name = MessageColumnName.SUBJECT) val subject: String,
    @ColumnInfo(name = MessageColumnName.BODY) val body: String,
    // A name of this column is "`index`"
    @PrimaryKey(autoGenerate = true) val index: Int,
)

class MessageColumnName {
    companion object {
        const val ADDRESS_TO = "addressTo"
        const val ADDRESS_CC = "addressCC"
        const val ADDRESS_BCC = "addressBCC"
        const val SUBJECT = "subject"
        const val BODY = "body"
        const val INDEX = "`index`"

        fun valueToName(value: String, context: Context): String {
            return when (value) {
                context.getString(R.string.none) -> INDEX
                context.getString(R.string.address_To) -> ADDRESS_TO
                context.getString(R.string.address_CC) -> ADDRESS_CC
                context.getString(R.string.address_BCC) -> ADDRESS_BCC
                context.getString(R.string.title) -> SUBJECT
                context.getString(R.string.body) -> BODY
                else -> throw Exception("${value::class.java.simpleName}($value) cannot be converted.")
            }
        }
    }
}
