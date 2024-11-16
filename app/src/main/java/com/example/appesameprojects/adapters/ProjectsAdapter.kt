package com.example.appesameprojects.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.appesameprojects.R
import com.example.appesameprojects.models.ProjectModel

class ProjectsAdapter(
    private val projectList: List<ProjectModel>,
    private val accountType: String?,
    private val onProjectClick: (ProjectModel) -> Unit,
    private val onReminderProjectClick: (ProjectModel) -> Unit
) : RecyclerView.Adapter<ProjectsAdapter.ProjectViewHolder>() {

    class ProjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val projectName: TextView = itemView.findViewById(R.id.project_name)
        val projectLeader: TextView = itemView.findViewById(R.id.project_leader)
        val expirationDate: TextView = itemView.findViewById(R.id.expiration_date)
        val projectProgressBar: ProgressBar = itemView.findViewById(R.id.project_progress_bar)
        val reminderIcon: ImageView = itemView.findViewById(R.id.reminder_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_project, parent, false)
        return ProjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        val currentProject = projectList[position]
        holder.projectName.text = currentProject.name
        holder.projectLeader.text = currentProject.projectLeaderName
        holder.expirationDate.text = currentProject.expirationDate

        // Imposta il progresso della ProgressBar
        val progressValue = currentProject.projectProgress?.removeSuffix("%")?.toIntOrNull() ?: 0
        holder.projectProgressBar.progress = progressValue

        // Imposta l'icona reminder per il Project Manager
        if (accountType == "Project Manager") {
            holder.reminderIcon.visibility = View.VISIBLE
            // Gestione del click sull'icona reminder
            holder.reminderIcon.setOnClickListener {
                onReminderProjectClick(currentProject)
            }
        } else {
            holder.reminderIcon.visibility = View.GONE
        }

        // Gestione del click sul progetto
        holder.itemView.setOnClickListener {
            onProjectClick(currentProject)
        }
    }

    override fun getItemCount() = projectList.size
}
