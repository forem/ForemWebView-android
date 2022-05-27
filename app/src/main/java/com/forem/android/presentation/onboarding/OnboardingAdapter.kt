package com.forem.android.presentation.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.forem.android.databinding.OnboardingForemItemBinding
import com.forem.android.databinding.OnboardingHeaderItemBinding

private const val VIEW_TYPE_HEADER_ITEM = 1
private const val VIEW_TYPE_FOREM_ITEM = 2

/** Adapter to bind header and forems to [RecyclerView] inside [OnboardingFragment]. */
class OnboardingAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val onboardingItemViewModelList = ArrayList<OnboardingItemViewModel>()

    /** Helps to add [OnboardingItemViewModel] to display it in [OnboardingFragment]'s [RecyclerView]. */
    fun addItem(item: OnboardingItemViewModel) {
        onboardingItemViewModelList.add(item)
        notifyItemInserted(onboardingItemViewModelList.size - 1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER_ITEM -> {
                val inflater = LayoutInflater.from(parent.context)
                val binding =
                    OnboardingHeaderItemBinding.inflate(
                        inflater,
                        parent,
                        /* attachToParent= */ false
                    )
                OnboardingHeaderItemViewHolder(binding)
            }
            VIEW_TYPE_FOREM_ITEM -> {
                val inflater = LayoutInflater.from(parent.context)
                val binding =
                    OnboardingForemItemBinding.inflate(
                        inflater,
                        parent,
                        /* attachToParent= */ false
                    )
                OnboardingForemItemViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            VIEW_TYPE_HEADER_ITEM -> {
                (holder as OnboardingHeaderItemViewHolder).bind(
                    onboardingItemViewModelList[position] as OnboardingHeaderItemViewModel
                )
            }
            VIEW_TYPE_FOREM_ITEM -> {
                (holder as OnboardingForemItemViewHolder).bind(
                    onboardingItemViewModelList[position] as OnboardingForemItemViewModel
                )
            }
            else -> throw IllegalArgumentException("Invalid item view type: ${holder.itemViewType}")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (onboardingItemViewModelList[position]) {
            is OnboardingHeaderItemViewModel -> {
                VIEW_TYPE_HEADER_ITEM
            }
            is OnboardingForemItemViewModel -> {
                VIEW_TYPE_FOREM_ITEM
            }
            else -> throw IllegalArgumentException(
                "Invalid type of data at $position with item ${onboardingItemViewModelList[position]}"
            )
        }
    }

    override fun getItemCount(): Int {
        return onboardingItemViewModelList.size
    }

    private class OnboardingHeaderItemViewHolder(
        private val binding: OnboardingHeaderItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(viewModel: OnboardingHeaderItemViewModel) {
            binding.viewModel = viewModel
        }
    }

    private class OnboardingForemItemViewHolder(
        private val binding: OnboardingForemItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(viewModel: OnboardingForemItemViewModel) {
            binding.viewModel = viewModel
        }
    }
}
