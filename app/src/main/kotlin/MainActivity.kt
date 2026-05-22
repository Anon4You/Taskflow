package com.taskflow.app

import android.graphics.Paint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

// ── Data Model ──────────────────────────────────────────────
data class TodoItem(
    val id: String = "",
    var text: String = "",
    var isDone: Boolean = false,
    var priority: Int = 1, // 0=Low, 1=Medium, 2=High
    var category: String = "General",
    var dueDate: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    var completedAt: Long = 0L
)

enum class SortMode { DATE_NEWEST, DATE_OLDEST, PRIORITY_HIGH, PRIORITY_LOW, ALPHA }

// ── Persistence ─────────────────────────────────────────────
class TodoStore(private val ctx: android.content.Context) {
    private val prefs = ctx.getSharedPreferences("taskflow_data", 0)
    private val gson = com.google.gson.Gson()

    fun load(): MutableList<TodoItem> {
        val json = prefs.getString("todos", null) ?: return mutableListOf()
        val type = com.google.gson.reflect.TypeToken.getParameterized(
            MutableList::class.java, TodoItem::class.java
        ).type
        return gson.fromJson(json, type) ?: mutableListOf()
    }

    fun save(list: List<TodoItem>) {
        prefs.edit().putString("todos", gson.toJson(list)).apply()
    }
}

// ── MainActivity ────────────────────────────────────────────
class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var searchEdit: EditText
    private lateinit var chipGroup: ChipGroup
    private lateinit var statsText: TextView
    private lateinit var sortBtn: ImageButton

    private lateinit var store: TodoStore
    private var allTodos = mutableListOf<TodoItem>()
    private var currentFilter = "All"
    private var currentSearch = ""
    private var currentSort = SortMode.DATE_NEWEST

    private lateinit var adapter: TodoAdapter

    // Native methods
    external fun generateId(): String
    external fun getTimestamp(): String

    companion object {
        init { System.loadLibrary("taskflow") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        store = TodoStore(this)
        allTodos = store.load()

        recyclerView = findViewById(R.id.recyclerView)
        emptyText = findViewById(R.id.emptyText)
        fabAdd = findViewById(R.id.fabAdd)
        searchEdit = findViewById(R.id.searchEdit)
        chipGroup = findViewById(R.id.chipGroup)
        statsText = findViewById(R.id.statsText)
        sortBtn = findViewById(R.id.sortBtn)

        adapter = TodoAdapter(
            onToggle = { pos -> toggleTodo(pos) },
            onDelete = { pos -> deleteTodoWithUndo(pos) },
            onEdit = { pos -> showEditDialog(pos) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Swipe to delete
        val swipe = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {
                deleteTodoWithUndo(vh.adapterPosition)
            }
        }
        ItemTouchHelper(swipe).attachToRecyclerView(recyclerView)

        fabAdd.setOnClickListener { showAddDialog() }
        sortBtn.setOnClickListener { showSortDialog() }

        searchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearch = s?.toString()?.trim()?.lowercase() ?: ""
                refreshList()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        setupFilterChips()
        refreshList()
    }

    private fun setupFilterChips() {
        val categories = listOf("All", "General", "Work", "Personal", "Shopping", "Health")
        chipGroup.removeAllViews()
        for (cat in categories) {
            val chip = Chip(this).apply {
                text = cat
                isCheckable = true
                isChecked = cat == currentFilter
                setOnClickListener {
                    currentFilter = cat
                    setupFilterChips()
                    refreshList()
                }
            }
            chipGroup.addView(chip)
        }
    }

    private fun refreshList() {
        var filtered = allTodos.toMutableList()

        // Category filter
        if (currentFilter != "All") {
            filtered = filtered.filter { it.category == currentFilter }.toMutableList()
        }

        // Search filter
        if (currentSearch.isNotEmpty()) {
            filtered = filtered.filter {
                it.text.lowercase().contains(currentSearch)
            }.toMutableList()
        }

        // Sort
        when (currentSort) {
            SortMode.DATE_NEWEST -> filtered.sortByDescending { it.createdAt }
            SortMode.DATE_OLDEST -> filtered.sortBy { it.createdAt }
            SortMode.PRIORITY_HIGH -> filtered.sortByDescending { it.priority }
            SortMode.PRIORITY_LOW -> filtered.sortBy { it.priority }
            SortMode.ALPHA -> filtered.sortBy { it.text.lowercase() }
        }

        adapter.submitList(filtered.toList())
        updateStats()
        emptyText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun updateStats() {
        val total = allTodos.size
        val done = allTodos.count { it.isDone }
        val percent = if (total > 0) (done * 100 / total) else 0
        statsText.text = "$done/$total done ($percent%)"
    }

    private fun showAddDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_todo, null)
        val editTask = view.findViewById<TextInputEditText>(R.id.editTask)
        val spinnerCategory = view.findViewById<Spinner>(R.id.spinnerCategory)
        val spinnerPriority = view.findViewById<Spinner>(R.id.spinnerPriority)
        val btnPickDate = view.findViewById<Button>(R.id.btnPickDate)

        val categories = arrayOf("General", "Work", "Personal", "Shopping", "Health")
        spinnerCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)

        val priorities = arrayOf("Low", "Medium", "High")
        spinnerPriority.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, priorities)
        spinnerPriority.setSelection(1)

        var pickedDate = 0L
        btnPickDate.setOnClickListener {
            val cal = Calendar.getInstance()
            android.app.DatePickerDialog(this, { _, y, m, d ->
                cal.set(y, m, d, 23, 59)
                pickedDate = cal.timeInMillis
                btnPickDate.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(pickedDate))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        AlertDialog.Builder(this, R.style.TaskFlowDialog)
            .setTitle("New Task")
            .setView(view)
            .setPositiveButton("Add") { _, _ ->
                val text = editTask.text?.toString()?.trim() ?: ""
                if (text.isNotEmpty()) {
                    val todo = TodoItem(
                        id = generateId(),
                        text = text,
                        priority = spinnerPriority.selectedItemPosition,
                        category = spinnerCategory.selectedItem.toString(),
                        dueDate = pickedDate
                    )
                    allTodos.add(0, todo)
                    store.save(allTodos)
                    refreshList()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditDialog(pos: Int) {
        val item = adapter.currentList.getOrNull(pos) ?: return
        val realIndex = allTodos.indexOfFirst { it.id == item.id }
        if (realIndex == -1) return

        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_todo, null)
        val editTask = view.findViewById<TextInputEditText>(R.id.editTask)
        val spinnerCategory = view.findViewById<Spinner>(R.id.spinnerCategory)
        val spinnerPriority = view.findViewById<Spinner>(R.id.spinnerPriority)
        val btnPickDate = view.findViewById<Button>(R.id.btnPickDate)

        editTask.setText(item.text)

        val categories = arrayOf("General", "Work", "Personal", "Shopping", "Health")
        spinnerCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spinnerCategory.setSelection(categories.indexOf(item.category).coerceAtLeast(0))

        val priorities = arrayOf("Low", "Medium", "High")
        spinnerPriority.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, priorities)
        spinnerPriority.setSelection(item.priority)

        var pickedDate = item.dueDate
        if (pickedDate > 0) {
            btnPickDate.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(pickedDate))
        }

        btnPickDate.setOnClickListener {
            val cal = Calendar.getInstance()
            if (pickedDate > 0) cal.timeInMillis = pickedDate
            android.app.DatePickerDialog(this, { _, y, m, d ->
                cal.set(y, m, d, 23, 59)
                pickedDate = cal.timeInMillis
                btnPickDate.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(pickedDate))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        AlertDialog.Builder(this, R.style.TaskFlowDialog)
            .setTitle("Edit Task")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val text = editTask.text?.toString()?.trim() ?: ""
                if (text.isNotEmpty()) {
                    allTodos[realIndex].text = text
                    allTodos[realIndex].priority = spinnerPriority.selectedItemPosition
                    allTodos[realIndex].category = spinnerCategory.selectedItem.toString()
                    allTodos[realIndex].dueDate = pickedDate
                    store.save(allTodos)
                    refreshList()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleTodo(pos: Int) {
        val item = adapter.currentList.getOrNull(pos) ?: return
        val idx = allTodos.indexOfFirst { it.id == item.id }
        if (idx == -1) return
        allTodos[idx].isDone = !allTodos[idx].isDone
        allTodos[idx].completedAt = if (allTodos[idx].isDone) System.currentTimeMillis() else 0
        store.save(allTodos)
        refreshList()
    }

    private fun deleteTodoWithUndo(pos: Int) {
        val item = adapter.currentList.getOrNull(pos) ?: return
        val idx = allTodos.indexOfFirst { it.id == item.id }
        if (idx == -1) return

        val removed = allTodos.removeAt(idx)
        store.save(allTodos)
        refreshList()

        Snackbar.make(recyclerView, "\"${removed.text}\" deleted", Snackbar.LENGTH_LONG)
            .setAction("UNDO") {
                allTodos.add(idx, removed)
                store.save(allTodos)
                refreshList()
            }
            .show()
    }

    private fun showSortDialog() {
        val options = arrayOf("Newest first", "Oldest first", "Priority: High→Low", "Priority: Low→High", "Alphabetical")
        AlertDialog.Builder(this, R.style.TaskFlowDialog)
            .setTitle("Sort by")
            .setItems(options) { _, which ->
                currentSort = SortMode.entries[which]
                refreshList()
            }
            .show()
    }
}

// ── Adapter with DiffUtil ───────────────────────────────────
class TodoAdapter(
    private val onToggle: (Int) -> Unit,
    private val onDelete: (Int) -> Unit,
    private val onEdit: (Int) -> Unit
) : ListAdapter<TodoItem, TodoAdapter.VH>(DiffCallback()) {

    private val priorityColors = intArrayOf(
        0xFF39FF14.toInt(), // Low - green
        0xFFFFF700.toInt(), // Medium - yellow
        0xFFFF006E.toInt()  // High - pink
    )

    private val priorityLabels = arrayOf("Low", "Med", "High")

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val card: View = view.findViewById(R.id.todoCard)
        val priorityBar: View = view.findViewById(R.id.priorityBar)
        val checkBox: CheckBox = view.findViewById(R.id.checkBox)
        val todoText: TextView = view.findViewById(R.id.todoText)
        val categoryBadge: TextView = view.findViewById(R.id.categoryBadge)
        val dueDateText: TextView = view.findViewById(R.id.dueDateText)
        val priorityLabel: TextView = view.findViewById(R.id.priorityLabel)
        val editBtn: ImageButton = view.findViewById(R.id.editBtn)
        val deleteBtn: ImageButton = view.findViewById(R.id.deleteBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_todo, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = currentList[position]

        holder.todoText.text = item.text
        holder.checkBox.isChecked = item.isDone

        // Priority bar color
        holder.priorityBar.setBackgroundColor(priorityColors[item.priority])
        holder.priorityLabel.text = priorityLabels[item.priority]
        holder.priorityLabel.setTextColor(priorityColors[item.priority])

        // Category badge
        holder.categoryBadge.text = item.category

        // Due date
        if (item.dueDate > 0) {
            val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
            holder.dueDateText.text = sdf.format(Date(item.dueDate))
            holder.dueDateText.visibility = View.VISIBLE
            // Highlight overdue
            if (item.dueDate < System.currentTimeMillis() && !item.isDone) {
                holder.dueDateText.setTextColor(0xFFFF006E.toInt())
            } else {
                holder.dueDateText.setTextColor(0xB3FFFFFF.toInt())
            }
        } else {
            holder.dueDateText.visibility = View.GONE
        }

        // Strikethrough when done
        if (item.isDone) {
            holder.todoText.paintFlags = holder.todoText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.todoText.alpha = 0.4f
            holder.card.alpha = 0.6f
        } else {
            holder.todoText.paintFlags = holder.todoText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.todoText.alpha = 1f
            holder.card.alpha = 1f
        }

        holder.checkBox.setOnClickListener { onToggle(holder.adapterPosition) }
        holder.editBtn.setOnClickListener { onEdit(holder.adapterPosition) }
        holder.deleteBtn.setOnClickListener { onDelete(holder.adapterPosition) }
    }

    class DiffCallback : DiffUtil.ItemCallback<TodoItem>() {
        override fun areItemsTheSame(a: TodoItem, b: TodoItem) = a.id == b.id
        override fun areContentsTheSame(a: TodoItem, b: TodoItem) = a == b
    }
}
