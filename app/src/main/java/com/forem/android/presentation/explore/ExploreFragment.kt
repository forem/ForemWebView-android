package com.forem.android.presentation.explore

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.databinding.ObservableField
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.forem.android.R
import com.forem.android.app.model.AddForemFragmentResult
import com.forem.android.app.model.DestinationScreen
import com.forem.android.app.model.Forem
import com.forem.android.app.model.PreviewFragmentData
import com.forem.android.data.repository.MyForemRepository
import com.forem.android.databinding.ExploreFragmentBinding
import com.forem.android.presentation.onboarding.OnboardingActivity
import com.forem.android.utility.Resource
import com.forem.android.utility.accessibility.AccessibilityChecker
import com.forem.android.utility.putProto
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Bottom sheet which helps in browsing local and remote forems. */
@AndroidEntryPoint
class ExploreFragment :
    Fragment(),
    ForemSelectedListener,
    DeleteForemViaAccessibilityListener,
    RecyclerItemTouchHelper.RecyclerItemTouchHelperListener {

    companion object {
        private const val RESULT_LISTENER_KEY = "ExploreFragment.resultListener"
        private const val RESULT_LISTENER_BUNDLE_KEY = "ExploreFragment.resultListenerBundle"

        /** Creates a new instance of [ExploreFragment]. */
        fun newInstance(
            resultListenerKey: String,
            resultListenerBundleKey: String
        ): ExploreFragment {
            val fragment = ExploreFragment()
            val args = Bundle()
            args.putString(RESULT_LISTENER_KEY, resultListenerKey)
            args.putString(RESULT_LISTENER_BUNDLE_KEY, resultListenerBundleKey)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var resultListenerKey: String
    private lateinit var resultListenerBundleKey: String

    private lateinit var binding: ExploreFragmentBinding
    private lateinit var viewModel: ExploreFragmentViewModel

    private lateinit var exploreAdapter: ExploreAdapter

    private lateinit var snackbar: Snackbar

    private val currentForem = ObservableField(Forem.getDefaultInstance())
    private var currentForemPosition = -1
    private val localForemList = ArrayList<Forem>()
    private val fullRemoteForemList = ArrayList<Forem>()
    private val partialRemoteForemList = ArrayList<Forem>()
    private val exploreItemViewModelList = ArrayList<ExploreItemViewModel>()

    private val currentFilter = ObservableField(FilterEnum.MY_LIST)

    private val itemTouchHelperCallback =
        RecyclerItemTouchHelper(dragDirs = 0, swipeDirs = ItemTouchHelper.START, listener = this)

    @Inject
    lateinit var myForemRepository: MyForemRepository

    private var isAccessibilityEnabled: Boolean = false

    private val currentForemFetched = ObservableField(false)
    private val localForemsFetched = ObservableField(false)
    private val remoteForemsFetched = ObservableField(false)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val args = requireArguments()
        resultListenerKey = args.getString(RESULT_LISTENER_KEY)!!
        resultListenerBundleKey = args.getString(RESULT_LISTENER_BUNDLE_KEY)!!

        isAccessibilityEnabled = AccessibilityChecker.isScreenReaderEnabled(this.requireContext())

        binding = ExploreFragmentBinding.inflate(
            inflater,
            container,
            /* attachToRoot= */ false
        )

        viewModel = ViewModelProvider(this).get(ExploreFragmentViewModel::class.java)
        binding.main = this
        binding.viewModel = viewModel

        binding.tabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val id = tab.position
                if (id == 0) {
                    viewModel.errorText.set("")
                    currentFilter.set(FilterEnum.MY_LIST)
                } else if (id == 1) {
                    currentFilter.set(FilterEnum.EXPLORE)
                    if (::snackbar.isInitialized) {
                        snackbar.dismiss()
                    }
                }
                recyclerViewDataSetChanged()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        binding.tabLayout.getTabAt(currentFilter.get()!!.ordinal)?.select()

        val linearLayoutManager = LinearLayoutManager(this.requireContext())
        binding.foremRecyclerView.apply {
            layoutManager = linearLayoutManager
        }

        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.foremRecyclerView)

        addCustomAccessibilityActions()

        setupLocalForemListObservers()
        setupCurrentForemObservers()
        setupRemoteForemListObservers()

        return binding.root
    }

    private fun addCustomAccessibilityActions() {
        ViewCompat.addAccessibilityAction(
            binding.exploreFragmentRootLayout,
            this.getString(R.string.add_forem_by_domain)
        ) { _, _ ->
            onAddForemByDomainButtonClicked()
            true
        }
    }

    fun selectMyListTab() {
        binding.tabLayout.getTabAt(0)?.select()
    }

    private fun setupCurrentForemObservers() {
        viewModel.currentForem.observe(
            viewLifecycleOwner
        ) {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    currentForem.set(it.data?.forem!!)
                    currentForemPosition = localForemList.indexOf(currentForem.get()!!)
                }
                Resource.Status.ERROR -> {
                    // TODO: Show proper UI for errors and log this error.
                }
                Resource.Status.LOADING -> {
                    // TODO: Show loading screen
                }
            }
            currentForemFetched.set(true)
            loadRecyclerViewIfPossible()
        }
    }

    private fun setupLocalForemListObservers() {
        viewModel.myForemCollection.observe(
            viewLifecycleOwner
        ) {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    localForemList.clear()
                    val foremList: List<Forem> = it.data?.foremMap?.values!!.toList()
                    localForemList.addAll(foremList)

                    if (localForemList.isEmpty()) {
                        val intent = OnboardingActivity.newInstance(this.requireContext())
                        activity?.startActivity(intent)
                        activity?.finish()
                        return@observe
                    }
                }
                Resource.Status.ERROR -> {
                    // TODO: Show proper UI for errors and log this error.
                }
                Resource.Status.LOADING -> {
                    // TODO: Show loading screen
                }
            }
            localForemsFetched.set(true)
            loadRecyclerViewIfPossible()
        }
    }

    private fun setupRemoteForemListObservers() {
        viewModel.featuredForems.observe(
            viewLifecycleOwner
        ) {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    viewModel.loadingViewVisibility.set(false)
                    fullRemoteForemList.clear()

                    val foremList: List<Forem> = it.data?.foremMap?.values!!.toList()
                    fullRemoteForemList.addAll(foremList)
                }
                Resource.Status.ERROR -> {
                    viewModel.loadingViewVisibility.set(false)
                    // TODO: Show proper UI for errors and log this error.
                    Toast.makeText(
                        this.requireContext(),
                        getString(R.string.network_not_reachable),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                Resource.Status.LOADING -> {
                    viewModel.loadingViewVisibility.set(true)
                    if (currentFilter.get() == FilterEnum.EXPLORE) {
                        viewModel.errorText.set(resources.getString(R.string.error_all_forems_added))
                    }
                }
            }
            remoteForemsFetched.set(true)
            loadRecyclerViewIfPossible()
        }
    }

    private fun loadRecyclerViewIfPossible() {
        if (currentForemFetched.get()!! && localForemsFetched.get()!! && remoteForemsFetched.get()!!) {
            createPartialRemoteForemList()
        }
    }

    /** (Full Remote Forem List) - (Local Forem List) = (Partial Forem List) */
    private fun createPartialRemoteForemList() {
        partialRemoteForemList.clear()
        fullRemoteForemList.forEach { remoteForem ->
            val localForemAvailable = localForemList.find { localForem ->
                remoteForem.name == localForem.name
            }
            if (localForemAvailable == null) {
                partialRemoteForemList.add(remoteForem)
            }
        }
        // This check helps ensure that once local as well as remote forems are fetched than only
        // we populate the recyclerview.
        if (localForemList.isNotEmpty() && fullRemoteForemList.isNotEmpty()) {
            recyclerViewDataSetChanged()
        }
    }

    private fun recyclerViewDataSetChanged() {
        exploreAdapter = ExploreAdapter()
        exploreAdapter.setCurrentForem(currentForemPosition, currentForem.get()!!)
        binding.foremRecyclerView.adapter = exploreAdapter
        exploreItemViewModelList.clear()

        // Create forem list which will be displayed in recyclerview
        when (currentFilter.get()) {
            FilterEnum.MY_LIST -> {
                localForemList.forEachIndexed { index, forem ->
                    exploreItemViewModelList.add(
                        LocalForemItemViewModel(
                            forem,
                            index,
                            foremSelectedListener = this,
                            deleteForemViaAccessibilityListener = this,
                            isAccessibilityEnabled
                        )
                    )
                }
                itemTouchHelperCallback.isSwipeEnabled = true
                ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.foremRecyclerView)
            }
            FilterEnum.EXPLORE -> {
                partialRemoteForemList.forEach {
                    exploreItemViewModelList.add(
                        RemoteForemItemViewModel(
                            it,
                            foremSelectedListener = this
                        )
                    )
                }
                itemTouchHelperCallback.isSwipeEnabled = false
                ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.foremRecyclerView)
            }
            else -> {
                throw IllegalArgumentException("Invalid filter: ${currentFilter.get()}")
            }
        }

        exploreAdapter.updateList(exploreItemViewModelList)

        viewModel.errorText.set(
            if (currentFilter.get() == FilterEnum.EXPLORE &&
                exploreItemViewModelList.isNullOrEmpty() &&
                !viewModel.loadingViewVisibility.get()
            ) {
                resources.getString(R.string.error_all_forems_added)
            } else {
                ""
            }
        )
    }

    private fun changeCurrentForem(forem: Forem) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            myForemRepository.updateCurrentForem(forem)
            withContext(Dispatchers.Main) {
                onBackClicked()
            }
        }
    }

    override fun onForemSelected(forem: Forem) {
        when (currentFilter.get()) {
            FilterEnum.EXPLORE -> {
                val previewFragmentData = PreviewFragmentData.newBuilder().setForem(forem).build()
                sendDataToFragmentResultListener(DestinationScreen.PREVIEW, previewFragmentData)
            }
            FilterEnum.MY_LIST -> {
                changeCurrentForem(forem)
            }
        }
    }

    // For accessibility users only.
    override fun onDeleteForemClicked(forem: Forem, position: Int) {
        showConfirmDeleteAlertDialog(forem, position)
    }

    // For non-accessibility users only.
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder?, direction: Int, position: Int) {
        if (viewHolder is ExploreAdapter.LocalForemItemViewHolder) {
            val deleteForem = localForemList[position]
            exploreAdapter.notifyItemChanged(position)
            showConfirmDeleteAlertDialog(deleteForem, position)
        }
    }

    private fun showConfirmDeleteAlertDialog(forem: Forem, position: Int) {
        val dialogMessage = String.format(
            resources.getString(R.string.confirm_delete_forem_description),
            forem.name
        )

        val builder = AlertDialog.Builder(this.requireContext())
        builder.setTitle(R.string.confirm_delete)
        builder.setMessage(dialogMessage)
        builder.setPositiveButton(R.string.delete) { dialogInterface: DialogInterface, _ ->
            updateCurrentForemBeforeDeletion(forem, position)
            dialogInterface.dismiss()
        }
        builder.setNegativeButton(R.string.cancel) { dialogInterface: DialogInterface, _: Int ->
            exploreAdapter.notifyItemChanged(position)
            dialogInterface.dismiss()
        }

        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(true)
        alertDialog.show()
    }

    private fun updateCurrentForemBeforeDeletion(forem: Forem, position: Int) {
        when {
            // Case: If there is only 1 item in list which needs to be removed.
            localForemList.size == 1 -> {
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    myForemRepository.removeCurrentForem()
                    withContext(Dispatchers.Main) {
                        deleteForemConfirmed(forem, position)
                    }
                }
            }
            // Case: If selected forem is being deleted.
            currentForem.get()?.name == forem.name -> {
                val updateToForem = if (position > 0) {
                    localForemList[0]
                } else {
                    localForemList[1]
                }
                // Case: If not a first item and is a currentForem,
                // update current forem to just previous item
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    myForemRepository.updateCurrentForem(updateToForem)
                    withContext(Dispatchers.Main) {
                        deleteForemConfirmed(forem, position)
                    }
                }
            }
            else -> {
                // Case: If non-selected multi-list forem is getting deleted,
                // do not update current forem and delete the forem.
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    withContext(Dispatchers.Main) {
                        deleteForemConfirmed(forem, position)
                    }
                }
            }
        }
    }

    private fun deleteForemConfirmed(forem: Forem, position: Int) {
        localForemList.removeAt(position)
        exploreItemViewModelList.removeAt(position)
        exploreAdapter.removeItemAt(position)
        viewModel.deleteForem(forem)
    }

    fun onAddForemByDomainButtonClicked() {
        sendDataToFragmentResultListener(DestinationScreen.ADD_FOREM)
    }

    fun onBackClicked() {
        sendDataToFragmentResultListener(DestinationScreen.CLOSE_CURRENT)
    }

    fun onPassportClicked() {
        sendDataToFragmentResultListener(DestinationScreen.PASSPORT)
    }

    private fun sendDataToFragmentResultListener(
        destinationScreen: DestinationScreen? = DestinationScreen.UNSPECIFIED_SCREEN,
        previewFragmentData: PreviewFragmentData? = PreviewFragmentData.getDefaultInstance()
    ) {
        val addForemFragmentResult = AddForemFragmentResult.newBuilder()
            .setDestinationScreen(destinationScreen)
            .setPreviewFragmentData(previewFragmentData)
            .build()

        val bundle = Bundle()
        bundle.putProto(resultListenerBundleKey, addForemFragmentResult)
        setFragmentResult(resultListenerKey, bundle)
    }
}

enum class FilterEnum {
    MY_LIST,
    EXPLORE
}
