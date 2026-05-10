package com.example.firefighters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class DashboardActivity : AppCompatActivity() {

    private data class Item(val id: String, var name: String)

    private val items = mutableListOf<Item>()
    private val storageFile: File by lazy { File(filesDir, "items.json") }

    private lateinit var itemsContainer: LinearLayout
    private lateinit var emptyState: TextView
    private lateinit var itemsScroll: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.dashboard_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        itemsContainer = findViewById(R.id.items_container)
        emptyState = findViewById(R.id.empty_state)
        itemsScroll = findViewById(R.id.items_scroll)

        loadItems()
        renderAll()

        findViewById<MaterialButton>(R.id.btn_add_new_items).setOnClickListener {
            showAddItemDialog()
        }
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }
    }

    private fun showAddItemDialog() {
        promptForName(title = "Add item", initial = "") { name ->
            val item = Item(UUID.randomUUID().toString(), name)
            items.add(item)
            inflateRow(item)
            saveItems()
            refreshEmptyState()
            itemsScroll.post { itemsScroll.fullScroll(ScrollView.FOCUS_DOWN) }
        }
    }

    private fun showItemActions(item: Item) {
        MaterialAlertDialogBuilder(this)
            .setTitle(item.name)
            .setItems(arrayOf("Edit", "Delete")) { _, which ->
                when (which) {
                    0 -> showEditDialog(item)
                    1 -> confirmDelete(item)
                }
            }
            .show()
    }

    private fun showEditDialog(item: Item) {
        promptForName(title = "Edit item", initial = item.name) { newName ->
            item.name = newName
            val row = findRow(item.id) ?: return@promptForName
            row.findViewById<TextView>(R.id.item_name).text = newName
            saveItems()
        }
    }

    private fun confirmDelete(item: Item) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete \"${item.name}\"?")
            .setMessage("This can't be undone.")
            .setPositiveButton("Delete") { _, _ ->
                items.removeAll { it.id == item.id }
                findRow(item.id)?.let { itemsContainer.removeView(it) }
                saveItems()
                refreshEmptyState()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun promptForName(title: String, initial: String, onSubmit: (String) -> Unit) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_item, null)
        val input = view.findViewById<TextInputEditText>(R.id.input_item_name)
        input.setText(initial)
        input.setSelection(initial.length)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(view)
            .setPositiveButton("Save") { _, _ -> commit(input, onSubmit) }
            .setNegativeButton("Cancel", null)
            .create()

        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                commit(input, onSubmit)
                dialog.dismiss()
                true
            } else false
        }

        dialog.setOnShowListener {
            input.requestFocus()
            dialog.window?.setSoftInputMode(
                android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
            )
        }
        dialog.show()
    }

    private fun commit(input: TextInputEditText, onSubmit: (String) -> Unit) {
        val name = input.text?.toString()?.trim().orEmpty()
        if (name.isNotEmpty()) onSubmit(name)
    }

    private fun renderAll() {
        itemsContainer.removeAllViews()
        items.forEach { inflateRow(it) }
        refreshEmptyState()
    }

    private fun inflateRow(item: Item) {
        val row = LayoutInflater.from(this).inflate(R.layout.item_row, itemsContainer, false)
        row.tag = item.id
        row.findViewById<TextView>(R.id.item_name).text = item.name
        row.setOnClickListener { showItemActions(item) }
        itemsContainer.addView(row)
    }

    private fun findRow(id: String): View? {
        for (i in 0 until itemsContainer.childCount) {
            val child = itemsContainer.getChildAt(i)
            if (child.tag == id) return child
        }
        return null
    }

    private fun refreshEmptyState() {
        emptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun loadItems() {
        items.clear()
        if (!storageFile.exists()) return
        runCatching {
            val arr = JSONArray(storageFile.readText())
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                items.add(Item(obj.getString("id"), obj.getString("name")))
            }
        }
    }

    private fun saveItems() {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(JSONObject().put("id", item.id).put("name", item.name))
        }
        runCatching { storageFile.writeText(arr.toString()) }
    }
}
