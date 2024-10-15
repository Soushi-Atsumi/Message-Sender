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

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
interface MessageDao {
    companion object {
        private const val GET_ALL_QUERY = "SELECT * FROM message"
        const val GET_ALL_WITH_ORDER_QUERY = "$GET_ALL_QUERY ORDER BY %s"
        const val GET_ALL_WITH_ORDER_DESC_QUERY = "$GET_ALL_WITH_ORDER_QUERY DESC"
    }

    @Query("$GET_ALL_QUERY ORDER BY ${MessageColumnName.INDEX}")
    fun getAll(): List<Message>

    // 動的クエリは RawQuery を使用する（ORDER BY :column_name は効果が無く無視される）
    @RawQuery
    fun getAllWithOrder(supportSQLiteQuery: SupportSQLiteQuery): List<Message>

    @RawQuery
    fun getAllWithOrderDesc(supportSQLiteQuery: SupportSQLiteQuery): List<Message>

    // 見つからないときは null (APIリファレンスにないけど)
    @Query("SELECT * FROM message WHERE ${MessageColumnName.ADDRESS_TO} = :addressTo AND ${MessageColumnName.ADDRESS_CC} = :addressCC AND ${MessageColumnName.ADDRESS_BCC} = :addressBCC AND ${MessageColumnName.SUBJECT} = :subject AND ${MessageColumnName.BODY} = :body")
    fun get(addressTo: String, addressCC: String, addressBCC: String, subject: String, body: String): Message?

    @Update
    fun update(message: Message)

    @Insert
    fun insertAll(vararg messages: Message)

    @Delete
    fun delete(message: Message)

    @Query("DELETE FROM message")
    fun deleteAll()
}