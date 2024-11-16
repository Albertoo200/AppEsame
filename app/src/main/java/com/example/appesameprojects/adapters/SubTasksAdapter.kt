package com.example.appesameprojects.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.appesameprojects.R
import com.example.appesameprojects.models.SubTaskModel

class SubTasksAdapter(
    private val subTaskList: List<SubTaskModel>,
    private val onSubTaskClick: (SubTaskModel) -> Unit
) : RecyclerView.Adapter<SubTasksAdapter.SubTaskViewHolder>() {

    class SubTaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val projectName: TextView = itemView.findViewById(R.id.project_name)
        val taskName: TextView = itemView.findViewById(R.id.task_name)
        val subTaskName: TextView = itemView.findViewById(R.id.subtask_name)
        val subTaskPriority: TextView = itemView.findViewById(R.id.subtask_priority)
        val subTaskStatus: TextView = itemView.findViewById(R.id.subtask_status)
        val subTaskExpirationDate: TextView = itemView.findViewById(R.id.subtask_expiration_date)
        val subTaskProgressBar: ProgressBar = itemView.findViewById(R.id.subtask_progress_bar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubTaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_subtask, parent, false)
        return SubTaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubTaskViewHolder, position: Int) {
        val currentSubTask = subTaskList[position]
        holder.projectName.text = currentSubTask.projectName
        holder.taskName.text = currentSubTask.taskName
        holder.subTaskName.text = currentSubTask.name
        holder.subTaskPriority.text = currentSubTask.priority
        holder.subTaskStatus.text = currentSubTask.status
        holder.subTaskExpirationDate.text = currentSubTask.expirationDate

        // Imposta il progresso della ProgressBar
        val progressValue = currentSubTask.subTaskProgress?.removeSuffix("%")?.toInt() ?: 0
        holder.subTaskProgressBar.progress = progressValue

        // Gestione del click sul subtask
        holder.itemView.setOnClickListener {
            onSubTaskClick(currentSubTask)
        }
    }

    override fun getItemCount() = subTaskList.size
}
