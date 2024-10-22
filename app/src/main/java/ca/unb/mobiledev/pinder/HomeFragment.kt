package ca.unb.mobiledev.pinder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import ca.unb.mobiledev.pinder.databinding.FragmentHomeBinding

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