package com.example.appesameprojects.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.appesameprojects.R
import com.example.appesameprojects.models.SkillModel
import com.example.appesameprojects.models.UserModel


class SkillsAdapter(
    private val skillList: List<SkillModel>
) : RecyclerView.Adapter<SkillsAdapter.SkillViewHolder>() {

    class SkillViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val skill: TextView = itemView.findViewById(R.id.skill_text_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkillViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_skill, parent, false)
        return SkillViewHolder(view)
    }

    override fun onBindViewHolder(holder: SkillViewHolder, position: Int) {
        val currentSkill = skillList[position]
        holder.skill.text = currentSkill.text
    }

    override fun getItemCount(): Int = skillList.size
}