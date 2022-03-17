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
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.sqlite.db.SimpleSQLiteQuery
import jp.soushiatsumi.messagesender.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var appIconDrawable: Drawable? = null
    val addOrEditOrDuplicateMessageActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refreshMessageRecyclerView(this)
    }

    private val preferencesActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refreshMessageRecyclerView(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.messageToolbar)
        binding.messageRecyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        refreshMessageRecyclerView(this)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.message_items, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.deleteAllItem -> {
                if (binding.messageRecyclerView.adapter?.itemCount ?: 0 > 0) {
                    AlertDialog.Builder(this).setTitle(getString(R.string.delete_all))
                        .setMessage(getString(R.string.are_you_sure)).apply {
                            setPositiveButton(R.string.yes) { _: DialogInterface, _: Int ->
                                runBlocking {
                                    withContext(Dispatchers.IO) {
                                        MessageDatabase.getDatabase(context).messageDao().deleteAll()
                                    }
                                    refreshMessageRecyclerView(context)
                                }
                            }
                            setNegativeButton(R.string.no) { _: DialogInterface, _: Int -> }
                        }.show()
                }
                return true
            }
            R.id.aboutItem -> {
                appIconDrawable = appIconDrawable ?: ResourcesCompat.getDrawable(resources, R.drawable.icon, null)
                val alertDialog = AlertDialog.Builder(this).setView(R.layout.about_this_app).show()
                alertDialog.findViewById<TextView>(R.id.versionNameTextView)!!.text = "${getString(R.string.version)}: ${BuildConfig.VERSION_NAME}"
                alertDialog.findViewById<ImageView>(R.id.appIconImageView)!!.setImageDrawable(appIconDrawable)
                return true
            }
            R.id.preferencesItem -> {
                preferencesActivityResultLauncher.launch(Intent(this, PreferencesActivity::class.java))
                return true
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

    private fun refreshMessageRecyclerView(context: Context) = runBlocking {
        val messageList = withContext(Dispatchers.IO) {
            val defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val orderTypeName = MessageColumnName.valueToName(defaultSharedPreferences.getString(context.getString(R.string.order_by), context.getString(R.string.none))!!, context)

            if (defaultSharedPreferences.getBoolean(context.getString(R.string.order_descending), false)) {
                val simpleSQLiteQuery = SimpleSQLiteQuery(MessageDao.GET_ALL_WITH_ORDER_DESC_QUERY.format(orderTypeName))
                MessageDatabase.getDatabase(context).messageDao().getAllWithOrderDesc(simpleSQLiteQuery)
            } else {
                val simpleSQLiteQuery = SimpleSQLiteQuery(MessageDao.GET_ALL_WITH_ORDER_QUERY.format(orderTypeName))
                MessageDatabase.getDatabase(context).messageDao().getAllWithOrder(simpleSQLiteQuery)
            }
        }

        (binding.messageRecyclerView.adapter as? MessageRecyclerViewAdapter)?.removeOnRefreshMessageRecyclerViewListener()
        val messageRecyclerViewAdapter = MessageRecyclerViewAdapter(messageList).apply {
            setOnRefreshMessageRecyclerViewListener(OnRefreshMessageRecyclerViewListener())
        }
        binding.messageRecyclerView.adapter = messageRecyclerViewAdapter
        (binding.messageRecyclerView.adapter as MessageRecyclerViewAdapter).notifyDataSetChanged()
    }

    fun addMessageFloatingActionButtonOnClick(view: View) {
        addOrEditOrDuplicateMessageActivityResultLauncher.launch(Intent(this, AddMessageActivity::class.java))
    }

    inner class OnRefreshMessageRecyclerViewListener {
        fun onRefresh(context: Context) {
            refreshMessageRecyclerView(context)
        }
    }

    fun githubLinkTextViewOnClick(view: View) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.link_github))))
    }
}