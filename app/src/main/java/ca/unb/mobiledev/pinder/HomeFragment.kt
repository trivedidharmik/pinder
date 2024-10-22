package ca.unb.mobiledev.pinder

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import ca.unb.mobiledev.pinder.databinding.FragmentHomeBinding
import com.google.android.material.snackbar.Snackbar

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ReminderViewModel by viewModels {
        ReminderViewModelFactory(requireActivity().application)
    }
    private lateinit var reminderAdapter: ReminderAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFloatingActionButton()
        observeReminders()
        setupSwipeToDelete()
    }

    private fun setupRecyclerView() {
        reminderAdapter = ReminderAdapter { reminder ->
            val bundle = Bundle().apply {
                putLong("reminderId", reminder.id)
            }
            findNavController().navigate(R.id.action_homeFragment_to_reminderCreationFragment, bundle)
        }
        binding.recyclerViewReminders.adapter = reminderAdapter
    }

    private fun setupSwipeToDelete() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val reminder = reminderAdapter.currentList[position]
                showDeleteConfirmationDialog(reminder)
            }
        }

        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerViewReminders)
    }

    private fun showDeleteConfirmationDialog(reminder: Reminder) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Reminder")
            .setMessage("Are you sure you want to delete this reminder?")
            .setPositiveButton("Delete") { _, _ ->
                deleteReminder(reminder)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                // Refresh the adapter to reset the swiped item
                reminderAdapter.notifyDataSetChanged()
            }
            .setOnCancelListener {
                // Refresh the adapter to reset the swiped item
                reminderAdapter.notifyDataSetChanged()
            }
            .show()
    }

    private fun deleteReminder(reminder: Reminder) {
        viewModel.deleteReminder(reminder.id)
        Snackbar.make(
            binding.root,
            "Reminder deleted",
            Snackbar.LENGTH_LONG
        ).setAction("UNDO") {
            viewModel.addReminder(reminder)
        }.show()
    }

    private fun setupFloatingActionButton() {
        binding.fabAddReminder.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_reminderCreationFragment)
        }
    }

    private fun observeReminders() {
        viewModel.reminders.observe(viewLifecycleOwner) { reminders ->
            reminderAdapter.submitList(reminders)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}