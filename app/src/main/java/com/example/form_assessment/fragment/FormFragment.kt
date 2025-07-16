package com.example.form_assessment.fragment
import android.Manifest
import android.R
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.children
import com.example.form_assessment.databinding.FragmentFormBinding
import com.example.form_assessment.firebase.FirebaseHelper
import com.example.form_assessment.viewmodel.FormViewModel
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.form_assessment.ResultActivity
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream


class FormFragment : Fragment() {
    private lateinit var binding: FragmentFormBinding
    private val viewModel: FormViewModel by viewModels()

    private val CAMERA_PERMISSION_CODE = 101
    private val CAMERA_REQUEST_CODE = 102
    private var cameraAnswerQuestionId: String? = null

    private var cameraImageBitmap: Bitmap? = null
    private var currentCameraImageId: String? = null



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        viewModel.loadSurvey()

        observeConnectivity()

        viewModel.currentQuestion.observe(viewLifecycleOwner) { question ->
            binding.tvQuestion.text = question.question.slug
            Log.d("answer", "onViewCreated: "+viewModel.getAnswerSubmissions())
            binding.optionsContainer.removeAllViews()

            binding.btnSkip.visibility = if (question.skip?.id != "-1") View.VISIBLE else View.GONE

            binding.btnNext.text = if(question.referTo?.id == "submit") "Submit" else "Next"

            when (question.type) {
                "multipleChoice" -> {
                    question.options?.forEach { opt ->
                        val radio = RadioButton(requireContext())
                        radio.text = opt.value
                        radio.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                        radio.textSize = 18f
                        radio.setOnClickListener {
                            viewModel.answerCurrent(question.id, opt.value)
                            binding.btnNext.setOnClickListener {
                                opt.referTo?.id?.let {
                                    checkAndSave(it)
                                }
                            }
                        }
                        binding.optionsContainer.addView(radio)
                    }
                }
                "textInput" -> {
                    val input = EditText(requireContext())
                    input.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                    input.textSize = 18f
                    binding.optionsContainer.addView(input)
                    binding.btnNext.setOnClickListener {
                        viewModel.answerCurrent(question.id, input.text.toString())
                        question.referTo?.id?.let { checkAndSave(it) }
                    }
                }
                "numberInput" -> {
                    val input = EditText(requireContext())
                    input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
                    input.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                    input.textSize = 18f
                    binding.optionsContainer.addView(input)
                    binding.btnNext.setOnClickListener {
                        viewModel.answerCurrent(question.id, input.text.toString())
                        question.referTo?.id?.let { checkAndSave(it) }
                    }
                }
                "checkbox" -> {
                    question.options?.forEach { opt ->
                        val check = CheckBox(requireContext())
                        check.text = opt.value
                        check.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                        check.textSize = 18f
                        binding.optionsContainer.addView(check)
                    }
                    binding.btnNext.setOnClickListener {
                        val selected = binding.optionsContainer.children
                            .filterIsInstance<CheckBox>()
                            .filter { it.isChecked }
                            .joinToString(", ") { it.text.toString() }

                        viewModel.answerCurrent(question.id, selected)
                        question.referTo?.id?.let { checkAndSave(it) }
                    }
                }
                "dropdown" -> {
                    val spinner = Spinner(requireContext())
                    val options = question.options ?: emptyList()
                    val adapter = ArrayAdapter(
                        requireContext(),
                        R.layout.simple_spinner_dropdown_item,
                        options.map { it.value }
                    )
                    spinner.adapter = adapter

                    binding.optionsContainer.addView(spinner)
                    binding.btnNext.setOnClickListener(null)
                    Log.d("dropdown", "onViewCreated: outside onClick")
                    binding.btnNext.setOnClickListener {
                        val selectedIndex = spinner.selectedItemPosition
                        if (selectedIndex == AdapterView.INVALID_POSITION) {
                            Toast.makeText(requireContext(), "Please select an option", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        val selectedOption = options[selectedIndex]
                        val selectedValue = selectedOption.value
                        val nextId = selectedOption.referTo?.id

                        viewModel.answerCurrent(question.id, selectedValue)
                        nextId.let { checkAndSave(it) }
                    }
                }
                "camera" -> {
                    val captureButton = Button(requireContext()).apply {
                        text = "Capture Image"
                    }

                    val retryButton = Button(requireContext()).apply {
                        text = "Retry"
                        visibility = View.GONE
                    }

                    val imageView = ImageView(requireContext()).apply {
                        layoutParams = ViewGroup.LayoutParams(400, 400)
                        visibility = View.GONE
                    }

                    cameraAnswerQuestionId = question.id

                    captureButton.setOnClickListener {
                        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                            != PackageManager.PERMISSION_GRANTED
                        ) {
                            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
                        } else {
                            openCamera()
                        }
                    }

                    retryButton.setOnClickListener {
                        openCamera()
                    }

                    binding.optionsContainer.addView(imageView)
                    binding.optionsContainer.addView(captureButton)
                    binding.optionsContainer.addView(retryButton)

                    // Save to use during submit
                    currentCameraImageId = question.id

                    binding.btnNext.setOnClickListener {
                        if (cameraImageBitmap == null) {
                            Toast.makeText(requireContext(), "Please capture an image", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
//                        viewModel.answerCurrent(question.id, "camera_captured") // Placeholder
                        question.referTo?.id?.let { checkAndSave(it) }
                    }
                }


                else -> { /* Other types like camera, dropdown, etc. */ }
            }

            binding.btnSkip.setOnClickListener {
                question.skip?.id?.let { viewModel.getNextQuestion(it) }
            }
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, CAMERA_REQUEST_CODE)
    }

    private fun checkAndSave(it: String?) {
        Log.d("answer", "checkAndSave: $it")
        if (it == "submit") {
            Log.d("answer", "checkAndSave: inside")

            if (isInternetAvailable(requireContext())) {
                FirebaseHelper.submitAnswers(
                    answers = viewModel.getAnswerSubmissions(),
                    onSuccess = {
                        lifecycleScope.launch {
                            viewModel.saveAnswersToRoom()
                        }
                        startActivity(Intent(requireContext(), ResultActivity::class.java))
                        requireActivity().finish()
                    },
                    onError = { error ->
                        Toast.makeText(requireContext(), "Submission failed: ${error.message}", Toast.LENGTH_LONG).show()
                        lifecycleScope.launch {
                            viewModel.saveAnswersToRoom()
                        }
                    }
                )
            } else {
                AlertDialog.Builder(requireContext())
                    .setTitle("No Internet Connection")
                    .setMessage("Do you want to save your answers locally?")
                    .setPositiveButton("Yes") { _, _ ->
                        lifecycleScope.launch {
                            viewModel.saveAnswersToRoom()
                            Toast.makeText(requireContext(), "Answers saved locally", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(requireContext(), ResultActivity::class.java))
                            requireActivity().finish()
                        }
                    }
                    .setNegativeButton("No", null)
                    .show()
            }

            Log.d("answer", "checkAndSave: outside")
        } else {
            viewModel.getNextQuestion(it)
        }
    }


    fun isInternetAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun observeConnectivity() {
        val cm = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // Internet became available → try syncing
                lifecycleScope.launch {
                    val synced = viewModel.syncLocalToFirebase()
                    if (synced) {
                        Log.d("Sync", "Synced local data to Firebase.")
                    }
                }
            }
        })
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val bitmap = data?.extras?.get("data") as? Bitmap ?: return

            cameraImageBitmap = bitmap

            // Save image to internal storage
            val filename = "img_${System.currentTimeMillis()}.jpg"
            val file = File(requireContext().filesDir, filename)
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            fos.close()

            val imagePath = file.absolutePath
            currentCameraImageId?.let {
                viewModel.answerCurrent(it, imagePath)
            }

            val imageView = binding.optionsContainer.children.find { it is ImageView } as? ImageView
            val retryButton = binding.optionsContainer.children.find { it is Button && it.text == "Retry" } as? Button

            imageView?.setImageBitmap(bitmap)
            imageView?.visibility = View.VISIBLE
            retryButton?.visibility = View.VISIBLE

            Toast.makeText(requireContext(), "Image saved locally", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }



}

