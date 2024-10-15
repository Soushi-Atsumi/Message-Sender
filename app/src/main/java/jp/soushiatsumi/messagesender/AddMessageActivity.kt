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

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.database.sqlite.SQLiteConstraintException
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Patterns
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import jp.soushiatsumi.messagesender.databinding.ActivityAddMessageBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class AddMessageActivity : AppCompatActivity() {
    companion object {
        val phoneNumberRegex: Regex = Regex("^\\+?[0-9-]+$")
    }

    private lateinit var binding: ActivityAddMessageBinding
    private lateinit var defaultEditTextBackground: Drawable
    private lateinit var backButtonCallback: OnBackPressedCallback
    private val isAllFieldsEmpty: Boolean
        get() = binding.addressToEditTextTextMessageAddress.text.isEmpty() &&
                binding.addressCCEditTextTextMessageAddress.text.isEmpty() &&
                binding.addressBCCEditTextTextMessageAddress.text.isEmpty() &&
                binding.subjectEditTextTextPersonName.text.isEmpty() &&
                binding.bodyEditTextTextMultiLine.text.isEmpty()
    private var addressToEditTextTextMessageAddressDefaultValue = ""
    private var addressCCEditTextTextMessageAddressDefaultValue = ""
    private var addressBCCEditTextTextMessageAddressDefaultValue = ""
    private var subjectEditTextTextPersonNameDefaultValue = ""
    private var bodyEditTextTextMultiLineDefaultValue = ""
    private val isAllFieldsDefaultValue: Boolean
        get() = binding.addressToEditTextTextMessageAddress.text.toString() == addressToEditTextTextMessageAddressDefaultValue &&
                binding.addressCCEditTextTextMessageAddress.text.toString() == addressCCEditTextTextMessageAddressDefaultValue &&
                binding.addressBCCEditTextTextMessageAddress.text.toString() == addressBCCEditTextTextMessageAddressDefaultValue &&
                binding.subjectEditTextTextPersonName.text.toString() == subjectEditTextTextPersonNameDefaultValue &&
                binding.bodyEditTextTextMultiLine.text.toString() == bodyEditTextTextMultiLineDefaultValue
    private var messageIndex: Int? = null
    private var isEditing = false
    private val choosingPhoneNumberActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            onAddingMessageResult(
                ContactPermissionRequestCode.SelectForAddressTo.ordinal,
                it.resultCode,
                it.data
            )
        }
    private val choosingAddressToActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            onAddingMessageResult(
                ContactPermissionRequestCode.SelectForAddressTo.ordinal,
                it.resultCode,
                it.data
            )
        }
    private val choosingAddressCCActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            onAddingMessageResult(
                ContactPermissionRequestCode.SelectForAddressCC.ordinal,
                it.resultCode,
                it.data
            )
        }
    private val choosingAddressBCCActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            onAddingMessageResult(
                ContactPermissionRequestCode.SelectForAddressBCC.ordinal,
                it.resultCode,
                it.data
            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddMessageBinding.inflate(layoutInflater)
        defaultEditTextBackground = binding.addressToEditTextTextMessageAddress.background
        val view = binding.root
        setContentView(view)

        val addressTo = intent.getStringExtra(IntentKey.AddressTo.name)
        val addressCC = intent.getStringExtra(IntentKey.AddressCC.name)
        val addressBCC = intent.getStringExtra(IntentKey.AddressBCC.name)
        val subject = intent.getStringExtra(IntentKey.Subject.name)
        val body = intent.getStringExtra(IntentKey.Body.name)
        val index = intent.getIntExtra(IntentKey.Index.name, -1)
        isEditing = intent.getBooleanExtra(IntentKey.IsEditing.name, false)
        setTitle(if (isEditing) R.string.edit_message else R.string.add_message)

        if (addressTo != null) {
            binding.addressToEditTextTextMessageAddress.setText(addressTo)
            addressToEditTextTextMessageAddressDefaultValue = addressTo
        }
        if (addressCC != null) {
            binding.addressCCEditTextTextMessageAddress.setText(addressCC)
            addressCCEditTextTextMessageAddressDefaultValue = addressCC
        }
        if (addressBCC != null) {
            binding.addressBCCEditTextTextMessageAddress.setText(addressBCC)
            addressBCCEditTextTextMessageAddressDefaultValue = addressBCC
        }
        if (subject != null) {
            binding.subjectEditTextTextPersonName.setText(subject)
            subjectEditTextTextPersonNameDefaultValue = subject
        }
        if (body != null) {
            binding.bodyEditTextTextMultiLine.setText(body)
            bodyEditTextTextMultiLineDefaultValue = body
        }
        if (index != -1) {
            messageIndex = index
        }

        // BackButton の動作を上書きする
        // onBackPressed を呼び出すとボタン押下と同じ挙動になるので、コールバックを無効化しないと無限ループする
        backButtonCallback = onBackPressedDispatcher.addCallback(this) {
            if (isAllFieldsDefaultValue) {
                finish()
            } else {
                askClosing()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // 親に戻るボタンはここで処理する
        // チュートリアルには Toolbar を作って云々書いてあるが、不要
        return if (isAllFieldsDefaultValue) {
            super.onOptionsItemSelected(item)
        } else {
            askClosing()
            true
        }
    }

    private fun onAddingMessageResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val contentUri = data?.data

        if (resultCode != RESULT_OK || contentUri == null) {
            return
        }

        val requestType = when (requestCode) {
            ContactPermissionRequestCode.SelectForPhoneNumber.ordinal -> ContactsContract.CommonDataKinds.Phone.NUMBER
            else -> ContactsContract.CommonDataKinds.Email.ADDRESS
        }
        val projection = arrayOf(requestType)
        val messageAddresses = mutableListOf<String>()
        try {
            // 一部の機種では依然として権限を要求する
            contentResolver.query(contentUri, projection, null, null, null).use {
                while (it?.moveToNext() == true) {
                    val columnIndexOfMessageAddress = it.getColumnIndex(requestType)
                    if (columnIndexOfMessageAddress >= 0) {
                        messageAddresses.add(it.getString(columnIndexOfMessageAddress))
                    }
                }
            }
        } catch (securityException: SecurityException) {
            AlertDialog.Builder(this).setTitle(R.string.failed_to_read).setMessage(getString(R.string.this_device_denied_reading)).show()
            return
        }

        messageAddresses.forEach {
            var targetEditText: EditText? = null
            when (requestCode) {
                ContactPermissionRequestCode.SelectForPhoneNumber.ordinal -> {
                    binding.addressToEditTextTextMessageAddress.setText(it)
                    return
                }
                ContactPermissionRequestCode.SelectForAddressTo.ordinal -> targetEditText =
                    binding.addressToEditTextTextMessageAddress
                ContactPermissionRequestCode.SelectForAddressCC.ordinal -> targetEditText =
                    binding.addressCCEditTextTextMessageAddress
                ContactPermissionRequestCode.SelectForAddressBCC.ordinal -> targetEditText =
                    binding.addressBCCEditTextTextMessageAddress
            }

            if (targetEditText?.text?.contains(it) == false) {
                if (targetEditText.text.isEmpty()) {
                    targetEditText.setText(it)
                } else {
                    targetEditText.setText("${targetEditText.text},$it")
                }
            }
        }
    }

    fun doneButtonOnClick(@Suppress("UNUSED_PARAMETER") view: View) {
        if (isAllFieldsEmpty) {
            AlertDialog.Builder(this).setMessage(getString(R.string.fill_in_one_field_at_least)).show()
            return
        } else if (isAllFieldsDefaultValue && isEditing) {
            finish()
            return
        }

        val addressTo = binding.addressToEditTextTextMessageAddress.text.toString()
        val addressCC = binding.addressCCEditTextTextMessageAddress.text.toString()
        val addressBCC = binding.addressBCCEditTextTextMessageAddress.text.toString()
        val subject = binding.subjectEditTextTextPersonName.text.toString()
        val body = binding.bodyEditTextTextMultiLine.text.toString()

        if (addressTo.isNotEmpty() && !validateMessageAddress(addressTo) && !validatePhoneNumber(addressTo)) {
            alertInvalidMessageAddressOrPhoneNumber()
            switchAlertingOfView(binding.addressToEditTextTextMessageAddress, true)
        } else if (addressCC.isNotEmpty() && !validateMessageAddress(addressCC)) {
            alertInvalidMessageAddress()
            switchAlertingOfView(binding.addressCCEditTextTextMessageAddress, true)
        } else if (addressBCC.isNotEmpty() && !validateMessageAddress(addressBCC)) {
            alertInvalidMessageAddress()
            switchAlertingOfView(binding.addressBCCEditTextTextMessageAddress, true)
        } else {
            val message = Message(addressTo, addressCC, addressBCC, subject, body, messageIndex ?: 0)
            val context: Context = this
            runBlocking {
                try {
                    val messageDao = MessageDatabase.getDatabase(context).messageDao()
                    if (withContext(Dispatchers.IO) { messageDao.get(message.addressTo, message.addressCC, message.addressBCC, message.subject, message.body) } != null) {
                        AlertDialog.Builder(context).setMessage(getString(R.string.this_combination_has_already_been_added)).show()
                    } else {
                        withContext(Dispatchers.IO) {
                            if (isEditing) {
                                messageDao.update(message)
                            } else {
                                messageDao.insertAll(message)
                            }
                        }

                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                } catch (e: SQLiteConstraintException) {
                    AlertDialog.Builder(context).setMessage(getString(R.string.it_has_not_been_able_to_be_added)).show()
                }
            }
        }
    }

    fun addressToEditTextTextMessageAddressOnClick(view: View) {
        switchAlertingOfView(view, false)
    }

    fun addressCCEditTextTextMessageAddressOnClick(view: View) {
        switchAlertingOfView(view, false)
    }

    fun addressBCCEditTextTextMessageAddressOnClick(view: View) {
        switchAlertingOfView(view, false)
    }

    fun chooseAddressToButtonOnClick(@Suppress("UNUSED_PARAMETER") view: View) {
        val contactsKindsTypeHashMap = hashMapOf(
            "Email" to ContactsContract.CommonDataKinds.Email.CONTENT_TYPE,
            "SMS" to ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
        )
        AlertDialog.Builder(this).setItems(contactsKindsTypeHashMap.keys.toTypedArray()) { _: DialogInterface, i: Int ->
            val contactsKindType = contactsKindsTypeHashMap[contactsKindsTypeHashMap.keys.elementAt(i)]
            val intent = Intent(Intent.ACTION_PICK).apply { type = contactsKindType }
            when (contactsKindType) {
                ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE -> choosingPhoneNumberActivityResultLauncher.launch(intent)
                else -> choosingAddressToActivityResultLauncher.launch(intent)
            }
        }.show()
    }

    fun chooseAddressCCButtonOnClick(@Suppress("UNUSED_PARAMETER") view: View) {
        val intent = Intent(Intent.ACTION_PICK).apply { type = ContactsContract.CommonDataKinds.Email.CONTENT_TYPE }
        choosingAddressCCActivityResultLauncher.launch(intent)
    }

    fun chooseAddressBCCButtonOnClick(@Suppress("UNUSED_PARAMETER") view: View) {
        val intent = Intent(Intent.ACTION_PICK).apply { type = ContactsContract.CommonDataKinds.Email.CONTENT_TYPE }
        choosingAddressBCCActivityResultLauncher.launch(intent)
    }

    private fun askClosing() {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.close_without_saving)).apply {
                setPositiveButton(R.string.yes) { _: DialogInterface, _: Int ->
                    finish()
                }
                setNegativeButton(R.string.no) { _: DialogInterface, _: Int -> }
            }.show()
    }

    private fun alertInvalidMessageAddress() {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.this_address_is_not_a_valid_format_of_a_mail))
            .show()
    }

    private fun alertInvalidMessageAddressOrPhoneNumber() {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.this_address_is_not_a_valid_format_of_a_mail_or_a_valid_format_of_a_phone_number))
            .show()
    }

    private fun switchAlertingOfView(view: View, isAlerting: Boolean) {
        if (isAlerting) {
            view.setBackgroundColor(ColorOnAlert.Error.color)
        } else {
            view.background = defaultEditTextBackground
        }
    }

    private fun validateMessageAddress(text: String): Boolean {
        return text.split(",").all { Patterns.EMAIL_ADDRESS.matcher(it).matches() }
    }

    private fun validatePhoneNumber(text: String): Boolean {
        return text.matches(phoneNumberRegex)
    }

    private enum class ColorOnAlert(val color: Int) {
        Error(Color.RED)
    }

    private enum class ContactPermissionRequestCode {
        SelectForPhoneNumber,
        SelectForAddressTo,
        SelectForAddressCC,
        SelectForAddressBCC,
    }

    enum class IntentKey {
        AddressTo,
        AddressCC,
        AddressBCC,
        Subject,
        Body,
        Index,
        IsEditing,
    }
}