package com.example.phanmanhtung_baitapcanhan.ui

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.phanmanhtung_baitapcanhan.databinding.DialogAddStudentBinding
import com.example.phanmanhtung_baitapcanhan.model.Student
import com.example.phanmanhtung_baitapcanhan.util.StudentValidator
import com.example.phanmanhtung_baitapcanhan.viewmodel.StudentViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale
import java.util.UUID

class AddStudentDialogFragment : DialogFragment() {
    private var _binding: DialogAddStudentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StudentViewModel by activityViewModels {
        (requireActivity() as MainActivity).studentFactory
    }
    private var requestId = ""
    private val editingId get() = arguments?.getInt("id", 0) ?: 0
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        requestId = state?.getString("request") ?: UUID.randomUUID().toString()
        // onCreateDialog không tạo Fragment View; dùng lifecycle DialogFragment, không gọi viewLifecycleOwner.
        viewModel.saveResult.observe(this) { result ->
            if (result.requestId == requestId) {
                if (result.error == null) dismiss()
                else _binding?.let { ui ->
                    ui.tvError.text = result.error
                    ui.tvError.isVisible = true
                }
            }
        }
        viewModel.saving.observe(this) { busy -> updateSaving(busy) }
    }
    override fun onCreateDialog(state: Bundle?): Dialog {
        _binding = DialogAddStudentBinding.inflate(layoutInflater)
        fun text(key: String, fallback: String = "") = state?.getString(key)
            ?: arguments?.getString(key) ?: fallback
        binding.etFullName.setText(text("name"))
        binding.etStudentCode.setText(text("code"))
        binding.etEmail.setText(text("email"))
        binding.etGpa.setText(text("gpa"))
        binding.etMajor.setText(text("major", "Công Nghệ Thông Tin"))
        binding.etFullName.doAfterTextChanged { binding.tilFullName.error = StudentValidator.nameError(name()) }
        binding.etStudentCode.doAfterTextChanged { binding.tilStudentCode.error = StudentValidator.codeError(code()) }
        binding.etEmail.doAfterTextChanged { binding.tilEmail.error = StudentValidator.emailError(email()) }
        binding.etGpa.doAfterTextChanged { binding.tilGpa.error = StudentValidator.gpaError(gpa()) }
        binding.etMajor.doAfterTextChanged { binding.tilMajor.error = StudentValidator.majorError(major()) }
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (editingId == 0) "Thêm sinh viên mới" else "Cập nhật sinh viên")
            .setView(binding.root).setNegativeButton("Hủy", null)
            .setPositiveButton("Lưu vào Room", null).create()
    }
    override fun onStart() {
        super.onStart()
        (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { save() }
        updateSaving(viewModel.saving.value == true)
    }
    private fun code() = binding.etStudentCode.text.toString().trim().uppercase(Locale.ROOT)
    private fun name() = binding.etFullName.text.toString().trim().replace(Regex("""\s+"""), " ")
    private fun email() = binding.etEmail.text.toString().trim().lowercase(Locale.ROOT)
    private fun gpa() = binding.etGpa.text.toString().trim()
    private fun major() = binding.etMajor.text.toString().trim()
    private fun save() {
        val errors = listOf(StudentValidator.nameError(name()), StudentValidator.codeError(code()),
            StudentValidator.emailError(email()), StudentValidator.gpaError(gpa()), StudentValidator.majorError(major()))
        binding.tilFullName.error = errors[0]; binding.tilStudentCode.error = errors[1]
        binding.tilEmail.error = errors[2]; binding.tilGpa.error = errors[3]; binding.tilMajor.error = errors[4]
        if (errors.any { it != null }) return
        binding.tvError.isVisible = false
        val student = Student(editingId, name(), code(), email(), StudentValidator.parseGpa(gpa())!!, major())
        if (editingId == 0) viewModel.insert(student, requestId) else viewModel.update(student, requestId)
    }
    private fun updateSaving(busy: Boolean) {
        val ui = _binding ?: return
        isCancelable = !busy
        (dialog as? AlertDialog)?.let { alert ->
            alert.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = !busy
            alert.getButton(AlertDialog.BUTTON_NEGATIVE)?.isEnabled = !busy
        }
        listOf(ui.etFullName, ui.etStudentCode, ui.etEmail, ui.etGpa, ui.etMajor).forEach { it.isEnabled = !busy }
    }
    override fun onSaveInstanceState(out: Bundle) {
        out.putString("request", requestId)
        _binding?.let { ui ->
            out.putString("name", ui.etFullName.text.toString()); out.putString("code", ui.etStudentCode.text.toString())
            out.putString("email", ui.etEmail.text.toString()); out.putString("gpa", ui.etGpa.text.toString())
            out.putString("major", ui.etMajor.text.toString())
        }
        super.onSaveInstanceState(out)
    }
    override fun onDestroyView() { _binding = null; super.onDestroyView() }
    companion object {
        fun newInstance(student: Student? = null) = AddStudentDialogFragment().apply {
            arguments = Bundle().apply {
                student?.let {
                    putInt("id", it.id); putString("name", it.fullName); putString("code", it.studentCode)
                    putString("email", it.email); putString("gpa", it.gpa.toString()); putString("major", it.major)
                }
            }
        }
    }
}