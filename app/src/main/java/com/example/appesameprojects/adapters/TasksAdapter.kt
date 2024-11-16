package com.example.appesameprojects.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.appesameprojects.R
import com.example.appesameprojects.models.ProjectModel
import com.example.appesameprojects.models.TaskModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class TasksAdapter(
    private val taskList: List<TaskModel>,
    private val accountType: String?,
    private val onTaskClick: (TaskModel) -> Unit,
    private val onReminderTaskClick: (TaskModel) -> Unit
) : RecyclerView.Adapter<TasksAdapter.TaskViewHolder>() {

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val projectName: TextView = itemView.findViewById(R.id.project_name)
        val taskName: TextView = itemView.findViewById(R.id.task_name)
        val taskDescription: TextView = itemView.findViewById(R.id.task_description)
        val taskDeveloper: TextView = itemView.findViewById(R.id.task_developer)
        val taskExpirationDate: TextView = itemView.findViewById(R.id.task_expiration_date)
        val taskProgressBar: ProgressBar = itemView.findViewById(R.id.task_progress_bar)
        val reminderIcon: ImageView = itemView.findViewById(R.id.reminder_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val currentTask = taskList[position]
        holder.projectName.text = currentTask.projectName
        holder.taskName.text = currentTask.name
        holder.taskDescription.text = currentTask.description
        holder.taskDeveloper.text = currentTask.developerName
        holder.taskExpirationDate.text = currentTask.expirationDate

        // Imposta il progresso della ProgressBar
        val progressValue = currentTask.taskProgress?.removeSuffix("%")?.toIntOrNull() ?: 0
        holder.taskProgressBar.progress = progressValue

        // Imposta l'icona reminder per il Project Leader
        if (accountType == "Project Leader") {
            holder.reminderIcon.visibility = View.VISIBLE

            // Gestione del click sull'icona reminder
            holder.reminderIcon.setOnClickListener {
                onReminderTaskClick(currentTask)
            }
        } else {
            holder.reminderIcon.visibility = View.GONE
        }

        // Gestione del click sul progetto
        holder.itemView.setOnClickListener {
            onTaskClick(currentTask)
        }
    }

    override fun getItemCount() = taskList.size
}
