package com.forem.android.presentation.explore

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.ObservableField
import androidx.recyclerview.widget.RecyclerView
import com.forem.android.app.model.Forem
import com.forem.android.databinding.LocalForemItemBinding
import com.forem.android.databinding.RemoteForemItemBinding

private const val VIEW_TYPE_LOCAL_FOREM_ITEM = 1
private const val VIEW_TYPE_REMOTE_FOREM_ITEM = 2

/** Adapter to bind local and remote forems to [RecyclerView] inside [ExploreFragment]. */
class ExploreAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val exploreItemViewModelList = ArrayList<ExploreItemViewModel>()

    private var currentlySelectedForemPosition = -1

    private val currentForem = ObservableField<Forem>()

    /** Sets the value of current forem which is used in [LocalForemItemViewModel]. */
    fun setCurrentForem(position: Int, forem: Forem) {
        currentForem.set(forem)
        if (currentlySelectedForemPosition >= 0) {
            notifyItemChanged(currentlySelectedForemPosition)
        }
        notifyItemChanged(position)
        currentlySelectedForemPosition = position
    }

    /** Helps to update entire list to display it in [ExploreFragment]'s [RecyclerView]. */
    fun updateList(exploreItemViewModelList: ArrayList<ExploreItemViewModel>) {
        this.exploreItemViewModelList.clear()
        this.exploreItemViewModelList.addAll(exploreItemViewModelList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_LOCAL_FOREM_ITEM -> {
                val inflater = LayoutInflater.from(parent.context)
                val binding =
                    LocalForemItemBinding.inflate(
                        inflater,
                        parent,
                        /* attachToParent= */ false
                    )
                LocalForemItemViewHolder(binding)
            }
            VIEW_TYPE_REMOTE_FOREM_ITEM -> {
                val inflater = LayoutInflater.from(parent.context)
                val binding =
                    RemoteForemItemBinding.inflate(
                        inflater,
                        parent,
                        /* attachToParent= */ false
                    )
                RemoteForemItemViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            VIEW_TYPE_LOCAL_FOREM_ITEM -> {
                val localForemItemViewModel =
                    exploreItemViewModelList[position] as LocalForemItemViewModel
                if (localForemItemViewModel.forem.name == currentForem.get()?.name) {
                    currentlySelectedForemPosition = holder.bindingAdapterPosition
                }
                (holder as LocalForemItemViewHolder).bind(
                    localForemItemViewModel,
                    currentForem.get()!!
                )
            }
            VIEW_TYPE_REMOTE_FOREM_ITEM -> {
                currentlySelectedForemPosition = -1
                (holder as RemoteForemItemViewHolder).bind(
                    exploreItemViewModelList[position] as RemoteForemItemViewModel
                )
            }
            else -> throw IllegalArgumentException("Invalid item view type: ${holder.itemViewType}")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (exploreItemViewModelList[position]) {
            is LocalForemItemViewModel -> {
                VIEW_TYPE_LOCAL_FOREM_ITEM
            }
            is RemoteForemItemViewModel -> {
                VIEW_TYPE_REMOTE_FOREM_ITEM
            }
            else -> throw IllegalArgumentException(
                "Invalid type of data at $position with item ${exploreItemViewModelList[position]}"
            )
        }
    }

    override fun getItemCount(): Int {
        return exploreItemViewModelList.size
    }

    fun removeItemAt(position: Int) {
        exploreItemViewModelList.removeAt(position)
        notifyItemRemoved(position)
    }

    fun addItemAt(position: Int, deletedItem: ExploreItemViewModel) {
        exploreItemViewModelList.add(position, deletedItem)
        notifyItemInserted(position)
    }

    class LocalForemItemViewHolder(
        private val binding: LocalForemItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        val foreground: ConstraintLayout = binding.foregroundConstraintLayout
        val background: ConstraintLayout = binding.backgroundConstraintLayout

        fun bind(viewModel: LocalForemItemViewModel, currentForem: Forem) {
            binding.viewModel = viewModel
            binding.currentForem = currentForem
        }
    }

    private class RemoteForemItemViewHolder(
        private val binding: RemoteForemItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(viewModel: RemoteForemItemViewModel) {
            binding.viewModel = viewModel
        }
    }
}
