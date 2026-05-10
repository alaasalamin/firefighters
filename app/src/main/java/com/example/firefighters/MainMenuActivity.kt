package com.example.firefighters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONArray
import java.io.File

class MainMenuActivity : AppCompatActivity() {

    private data class Item(val id: String, val name: String)

    private val counters = mutableMapOf<String, Int>()
    private lateinit var clickLog: TextView
    private lateinit var clickLogScroll: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_menu)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_menu_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        clickLog = findViewById(R.id.click_log)
        clickLogScroll = findViewById(R.id.click_log_scroll)

        val items = loadItems()
        val grid = findViewById<RecyclerView>(R.id.items_grid)
        val emptyState = findViewById<TextView>(R.id.empty_state)

        if (items.isEmpty()) {
            grid.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            grid.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
            grid.layoutManager = GridLayoutManager(this, 3)
            grid.adapter = ItemAdapter(items) { onItemClicked(it) }
        }

        findViewById<MaterialButton>(R.id.btn_print).setOnClickListener { showReceipt() }
        findViewById<MaterialButton>(R.id.btn_new_order).setOnClickListener { startNewOrder() }
    }

    private fun showReceipt() {
        val body = clickLog.text.toString().ifBlank { "No items in this order yet." }
        MaterialAlertDialogBuilder(this)
            .setTitle("Receipt")
            .setMessage(body)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun startNewOrder() {
        counters.clear()
        clickLog.text = ""
    }

    private fun onItemClicked(item: Item) {
        val count = (counters[item.id] ?: 0) + 1
        counters[item.id] = count
        clickLog.append("${count}x ${item.name}\n")
        clickLogScroll.post { clickLogScroll.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun loadItems(): List<Item> {
        val file = File(filesDir, "items.json")
        if (!file.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map {
                val obj = arr.getJSONObject(it)
                Item(obj.getString("id"), obj.getString("name"))
            }
        }.getOrDefault(emptyList())
    }

    private class ItemAdapter(
        private val items: List<Item>,
        private val onClick: (Item) -> Unit,
    ) : RecyclerView.Adapter<ItemAdapter.VH>() {

        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.item_name)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_box, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.name.text = item.name
            holder.itemView.setOnClickListener { onClick(item) }
        }

        override fun getItemCount() = items.size
    }
}
