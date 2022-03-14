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

import android.app.AlertDialog
import android.content.*
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class MessageRecyclerViewAdapter(private val dataSet: List<Message>) :
    RecyclerView.Adapter<MessageRecyclerViewAdapter.ViewHolder>() {
    private var onRefreshMessageRecyclerViewListener: MainActivity.OnRefreshMessageRecyclerViewListener? =
        null

    fun setOnRefreshMessageRecyclerViewListener(onRefreshMessageRecyclerViewListener: MainActivity.OnRefreshMessageRecyclerViewListener) {
        this.onRefreshMessageRecyclerViewListener = onRefreshMessageRecyclerViewListener
    }

    fun removeOnRefreshMessageRecyclerViewListener() {
        this.onRefreshMessageRecyclerViewListener = null
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val messageRowConstraintLayout: ConstraintLayout = view.findViewById(R.id.messageRowConstraintLayout)
        val addressToTextView: TextView = view.findViewById(R.id.addressToTextView)
        val addressToValueTextView: TextView = view.findViewById(R.id.addressToValueTextView)
        val addressCCTextView: TextView = view.findViewById(R.id.addressCCTextView)
        val addressCCValueTextView: TextView = view.findViewById(R.id.addressCCValueTextView)
        val addressBCCTextView: TextView = view.findViewById(R.id.addressBCCTextView)
        val addressBCCValueTextView: TextView = view.findViewById(R.id.addressBCCValueTextView)
        lateinit var message: Message
        val subjectRowTextView: TextView = view.findViewById(R.id.subjectRowTextView)
        val bodyRowTextView: TextView = view.findViewById(R.id.bodyRowTextView)

        init {
            messageRowConstraintLayout.setOnClickListener {
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, message.subject)
                    putExtra(Intent.EXTRA_TEXT, message.body)
                }

                if (message.addressTo.matches(AddMessageActivity.phoneNumberRegex)) {
                    intent.action = Intent.ACTION_SENDTO
                    intent.data = Uri.parse("smsto:${message.addressTo}")
                    intent.putExtra("sms_body", message.body)
                } else {
                    if (message.addressTo.isNotEmpty()) {
                        intent.putExtra(Intent.EXTRA_EMAIL, message.addressTo.split(",").toTypedArray())
                    }
                    if (message.addressCC.isNotEmpty()) {
                        intent.putExtra(Intent.EXTRA_CC, message.addressCC.split(",").toTypedArray())
                    }
                    if (message.addressBCC.isNotEmpty()) {
                        intent.putExtra(Intent.EXTRA_BCC, message.addressBCC.split(",").toTypedArray())
                    }
                }

                if (PreferenceManager.getDefaultSharedPreferences(view.context).getBoolean(view.context.getString(R.string.always_choose_app), false)) {
                    view.context.startActivity(Intent.createChooser(intent, null))
                } else {
                    if (intent.resolveActivity(view.context.packageManager) != null) {
                        view.context.startActivity(intent)
                    } else {
                        // 使用できるアプリがない場合、単純に「ありません」の窓が出る
                        view.context.startActivity(Intent.createChooser(intent, null))
                    }
                }
            }

            messageRowConstraintLayout.setOnLongClickListener {
                val copyAddressToSelection = "${view.context.getString(R.string.copy_address)}(To)"
                val copyAddressCCSelection = "${view.context.getString(R.string.copy_address)}(CC)"
                val copyAddressBCCSelection = "${view.context.getString(R.string.copy_address)}(BCC)"
                val copySubjectSelection = view.context.getString(R.string.copy_subject)
                val copyBodySelection = view.context.getString(R.string.copy_body)
                val copySelectionItems = arrayListOf(
                    copyAddressToSelection,
                    copyAddressCCSelection,
                    copyAddressBCCSelection,
                    copySubjectSelection,
                    copyBodySelection
                )
                val editSelection = view.context.getString(R.string.edit)
                val deleteSelection = view.context.getString(R.string.delete)
                val duplicateSelection = view.context.getString(R.string.duplicate)
                val shareAddressToSelection = "${view.context.getString(R.string.share_address)}(To)"
                val shareAddressCCSelection = "${view.context.getString(R.string.share_address)}(CC)"
                val shareAddressBCCSelection = "${view.context.getString(R.string.share_address)}(BCC)"
                val shareSubjectSelection = view.context.getString(R.string.share_subject)
                val shareBodySelection = view.context.getString(R.string.share_body)
                val selections: MutableList<String> = mutableListOf()

                if (message.addressTo.isNotEmpty()) {
                    selections.add(copyAddressToSelection)
                    selections.add(shareAddressToSelection)
                }
                if (message.addressCC.isNotEmpty()) {
                    selections.add(copyAddressCCSelection)
                    selections.add(shareAddressCCSelection)
                }
                if (message.addressBCC.isNotEmpty()) {
                    selections.add(copyAddressBCCSelection)
                    selections.add(shareAddressBCCSelection)
                }
                if (message.subject.isNotEmpty()) {
                    selections.add(copySubjectSelection)
                    selections.add(shareSubjectSelection)
                }
                if (message.body.isNotEmpty()) {
                    selections.add(copyBodySelection)
                    selections.add(shareBodySelection)
                }

                selections.addAll(arrayOf(editSelection, deleteSelection, duplicateSelection))

                AlertDialog.Builder(view.context).setItems(selections.toTypedArray()) { _: DialogInterface, i: Int ->
                    val clipboardManager = (view.context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)

                    if (copySelectionItems.contains(selections[i]) && (clipboardManager == null)) {
                        AlertDialog.Builder(view.context)
                            .setMessage(view.context.getString(R.string.cannot_access_the_clipboard))
                            .show()
                        return@setItems
                    }

                    when (selections[i]) {
                        copyAddressToSelection -> clipboardManager!!.setPrimaryClip((ClipData.newPlainText("address", message.addressTo)))
                        copyAddressCCSelection -> clipboardManager!!.setPrimaryClip((ClipData.newPlainText("address", message.addressCC)))
                        copyAddressBCCSelection -> clipboardManager!!.setPrimaryClip((ClipData.newPlainText("address", message.addressBCC)))
                        copySubjectSelection -> clipboardManager!!.setPrimaryClip((ClipData.newPlainText("subject", message.subject)))
                        copyBodySelection -> clipboardManager!!.setPrimaryClip((ClipData.newPlainText("body", message.body)))
                        editSelection -> {
                            val intent = Intent(view.context, AddMessageActivity::class.java).apply {
                                putExtra(AddMessageActivity.IntentKey.AddressTo.name, message.addressTo)
                                putExtra(AddMessageActivity.IntentKey.AddressCC.name, message.addressCC)
                                putExtra(AddMessageActivity.IntentKey.AddressBCC.name, message.addressBCC)
                                putExtra(AddMessageActivity.IntentKey.Subject.name, message.subject)
                                putExtra(AddMessageActivity.IntentKey.Body.name, message.body)
                                putExtra(AddMessageActivity.IntentKey.Index.name, message.index)
                                putExtra(AddMessageActivity.IntentKey.IsEditing.name, true)
                            }
                            (view.context as? MainActivity)?.addOrEditOrDuplicateMessageActivityResultLauncher?.launch(intent)
                        }
                        deleteSelection -> AlertDialog.Builder(view.context)
                            .setTitle(view.context.getString(R.string.delete))
                            .setMessage(view.context.getString(R.string.are_you_sure))
                            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                                runBlocking {
                                    withContext(Dispatchers.IO) {
                                        MessageDatabase.getDatabase(view.context).messageDao().delete(message)
                                    }
                                    onRefreshMessageRecyclerViewListener?.onRefresh(view.context)
                                }
                            }.setNegativeButton(android.R.string.cancel, null)
                            .show()
                        duplicateSelection -> {
                            val intent = Intent(view.context, AddMessageActivity::class.java).apply {
                                putExtra(AddMessageActivity.IntentKey.AddressTo.name, message.addressTo)
                                putExtra(AddMessageActivity.IntentKey.AddressCC.name, message.addressCC)
                                putExtra(AddMessageActivity.IntentKey.AddressBCC.name, message.addressBCC)
                                putExtra(AddMessageActivity.IntentKey.Subject.name, message.subject)
                                putExtra(AddMessageActivity.IntentKey.Body.name, message.body)
                            }
                            (view.context as? MainActivity)?.addOrEditOrDuplicateMessageActivityResultLauncher?.launch(intent)
                        }
                        shareAddressToSelection -> view.context.startActivity(createShareIntent(message.addressTo))
                        shareAddressCCSelection -> view.context.startActivity(createShareIntent(message.addressCC))
                        shareAddressBCCSelection -> view.context.startActivity(createShareIntent(message.addressBCC))
                        shareSubjectSelection -> view.context.startActivity(createShareIntent(message.subject))
                        shareBodySelection -> view.context.startActivity(createShareIntent(message.body))
                    }
                }.show()
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.message_recycler_view_row_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.message = Message(
            dataSet[position].addressTo,
            dataSet[position].addressCC,
            dataSet[position].addressBCC,
            dataSet[position].subject,
            dataSet[position].body,
            dataSet[position].index,
        )
        val defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(holder.itemView.context)
        if (holder.message.addressTo.isEmpty()) {
            if (defaultSharedPreferences.getBoolean(holder.itemView.context.getString(R.string.hide_empty_to), false)) {
                holder.addressToTextView.visibility = View.GONE
                holder.addressToValueTextView.visibility = View.GONE
            } else {
                holder.addressToValueTextView.text = holder.itemView.context.getString(R.string.no_addresses_To)
            }
        } else {
            holder.addressToValueTextView.text = holder.message.addressTo
        }
        if (holder.message.addressCC.isEmpty()) {
            if (defaultSharedPreferences.getBoolean(holder.itemView.context.getString(R.string.hide_empty_CC), false)) {
                holder.addressCCTextView.visibility = View.GONE
                holder.addressCCValueTextView.visibility = View.GONE
            } else {
                holder.addressCCValueTextView.text = holder.itemView.context.getString(R.string.no_addresses_CC)
            }
        } else {
            holder.addressCCValueTextView.text = holder.message.addressCC
        }
        if (holder.message.addressBCC.isEmpty()) {
            if (defaultSharedPreferences.getBoolean(holder.itemView.context.getString(R.string.hide_empty_BCC), false)) {
                holder.addressBCCTextView.visibility = View.GONE
                holder.addressBCCValueTextView.visibility = View.GONE
            } else {
                holder.addressBCCValueTextView.text = holder.itemView.context.getString(R.string.no_addresses_BCC)
            }
        } else {
            holder.addressBCCValueTextView.text = holder.message.addressBCC
        }
        if (holder.message.subject.isEmpty()) {
            if (defaultSharedPreferences.getBoolean(holder.itemView.context.getString(R.string.hide_empty_subject), false)) {
                holder.subjectRowTextView.visibility = View.GONE
            } else {
                holder.subjectRowTextView.text = holder.itemView.context.getString(R.string.no_subject)
            }
        } else {
            holder.subjectRowTextView.text = holder.message.subject
        }
        if (holder.message.body.isEmpty()) {
            if (defaultSharedPreferences.getBoolean(holder.itemView.context.getString(R.string.hide_empty_body), false)) {
                holder.bodyRowTextView.visibility = View.GONE
            } else {
                holder.bodyRowTextView.text = holder.itemView.context.getString(R.string.no_body)
            }
        } else {
            holder.bodyRowTextView.text = holder.message.body
        }
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    private fun createShareIntent(shareContent: String): Intent {
        return Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareContent)
            type = "text/plain"
        }, null)
    }
}