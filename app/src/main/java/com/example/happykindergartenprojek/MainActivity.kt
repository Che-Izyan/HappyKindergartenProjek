package com.example.happykindergartenprojek


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.happykindergartenprojek.ui.theme.HappyKindergartenProjekTheme
import androidx.compose.ui.tooling.preview.Preview
import io.github.jan.supabase.postgrest.postgrest
import java.text.SimpleDateFormat
import java.util.*

data class StudentInfo(
    val fullName: String,
    val nickname: String,
    val gender: String,
    val dob: String,
    val address: String,
    val religion: String,
    val nationality: String,
    val allergies: String,
    val chronicIllnesses: String,
    val className: String
)
data class ParentInfo(
    val childName: String,
    val fatherName: String,
    val fatherPhone: String,
    val fatherAddress: String,
    val motherName: String,
    val motherPhone: String,
    val motherAddress: String,
    val guardianName: String,
    val guardianPhone: String,
    val relation: String
)

data class StudentReport(
    val name: String,
    val literacy: String = "",
    val numeracy: String = "",
    val socialSkills: String = "",
    val physicalEducation: String = ""
)

data class AttendanceRecord(
    val name: String,
    val status: String = "",
    val remarks: String = "-"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HappyKindergartenProjekTheme {
                LaunchedEffect(Unit) {
                    try {
                        val result = SupabaseProvider.client.postgrest["students"].select()
                        println("SupabaseTest: Success!")
                    } catch (e: Exception) {
                        println("SupabaseTest: Failed: ${e.message}")
                    }
                }
                KindergartenApp()
            }
        }
    }
}

@Composable
fun KindergartenApp() {
    var screen by remember { mutableStateOf("login") }
    val parentList = remember { mutableStateListOf<ParentInfo>() }
    var updateIndex by remember { mutableIntStateOf(-1) }
    val studentList = remember { mutableStateListOf<StudentInfo>() }
    var studentUpdateIndex by remember { mutableIntStateOf(-1) }
    val reportList = remember {
        mutableStateListOf<StudentReport>()
    }
    val attendanceMap = remember { mutableStateMapOf<String, SnapshotStateList<AttendanceRecord>>() }
    var selectedDate by remember {
        mutableStateOf(SimpleDateFormat("d/M/yyyy", Locale.getDefault()).format(Date()))
    }

    fun getAttendanceForDate(date: String): SnapshotStateList<AttendanceRecord> {
        val list = attendanceMap.getOrPut(date) {
            mutableStateListOf<AttendanceRecord>().apply {
                studentList.forEach { add(AttendanceRecord(it.fullName)) }
            }
        }
        // Ensure list is in sync with studentList
        val studentNames = studentList.map { it.fullName }
        studentList.forEach { student ->
            if (list.none { it.name == student.fullName }) {
                list.add(AttendanceRecord(student.fullName))
            }
        }
        list.removeAll { record -> record.name !in studentNames }
        return list
    }

    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            onConfirm = {
                showLogoutDialog = false
                screen = "login"
            },
            onDismiss = { showLogoutDialog = false }
        )
    }


    when (screen) {
        "login" -> LoginScreen(onLogin = { screen = "main" })

        "main" -> MainMenuScreen(
            onLogout = { showLogoutDialog = true },
            onParentClick = { screen = "parentMenu" },
            onReportClick = { screen = "report" },
            onRegisterClick = {screen = "childMenu"},
            onAttendanceClick = { screen = "attendance" }
        )

        "parentMenu" -> ParentMenuScreen(
            onBack = { screen = "main" },
            onAddParent = {
                updateIndex = -1
                screen = "form"
            },
            onViewParent = { screen = "view" }
        )

        "form" -> ParentFormScreen(
            parent = if (updateIndex >= 0) parentList[updateIndex] else null,
            onBack = { screen = "parentMenu" },
            onSave = { parent ->
                if (updateIndex >= 0) {
                    parentList[updateIndex] = parent
                } else {
                    parentList.add(parent)
                }
                screen = "view"
            }
        )

        "view" -> ViewParentScreen(
            parentList = parentList,
            onBack = { screen = "parentMenu" },
            onUpdate = { index ->
                updateIndex = index
                screen = "form"
            },
            onDelete = { index ->
                parentList.removeAt(index)
            }
        )

        "childMenu" -> ChildMenuScreen(
            onBack = { screen = "main" },
            onAddStudent = {
                studentUpdateIndex = -1
                screen = "childForm"
            },
            onViewStudents = { screen = "childView" }
        )

        "childForm" -> ChildFormScreen(
            student = if (studentUpdateIndex >= 0) studentList[studentUpdateIndex] else null,
            onBack = { screen = "childMenu" },
            onSave = { student ->
                if (studentUpdateIndex >= 0) {
                    val oldName = studentList[studentUpdateIndex].fullName
                    studentList[studentUpdateIndex] = student
                    // Sync name in report
                    val reportIdx = reportList.indexOfFirst { it.name == oldName }
                    if (reportIdx >= 0) {
                        reportList[reportIdx] = reportList[reportIdx].copy(name = student.fullName)
                    }
                    // Sync name in attendance
                    attendanceMap.values.forEach { list ->
                        val attendanceIdx = list.indexOfFirst { it.name == oldName }
                        if (attendanceIdx >= 0) {
                            list[attendanceIdx] = list[attendanceIdx].copy(name = student.fullName)
                        }
                    }
                } else {
                    studentList.add(student)
                    // Auto add to report
                    reportList.add(StudentReport(name = student.fullName))
                    // New students will be auto-added to attendance when the date is viewed
                }
                screen = "childView"
            }
        )

        "childView" -> ViewChildScreen(
            studentList = studentList,
            onBack = { screen = "childMenu" },
            onUpdate = { index ->
                studentUpdateIndex = index
                screen = "childForm"
            },
            onDelete = { index ->
                val nameToDelete = studentList[index].fullName
                studentList.removeAt(index)
                // Remove from report too
                reportList.removeAll { it.name == nameToDelete }
                // Remove from attendance maps
                attendanceMap.values.forEach { list ->
                    list.removeAll { it.name == nameToDelete }
                }
            }
        )

        "report" -> ReportScreen(
            reportList = reportList,
            onBack = { screen = "main" },
            onEdit = { screen = "editReport" }
        )

        "editReport" -> EditReportScreen(
            reportList = reportList,
            onCancel = { screen = "main" },
            onDone = { screen = "report" }
        )

        "attendance" -> AttendanceScreen(
            attendanceList = getAttendanceForDate(selectedDate),
            selectedDate = selectedDate,
            onDateChange = { selectedDate = it },
            onBack = { screen = "main" },
            onEdit = { screen = "editAttendance" }
        )

        "editAttendance" -> EditAttendanceScreen(
            attendanceList = getAttendanceForDate(selectedDate),
            selectedDate = selectedDate,
            onDateChange = { selectedDate = it },
            onCancel = { screen = "main" },
            onDone = { screen = "attendance" }
        )
    }
}


@Composable
fun LoginScreen(onLogin: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "Logo",
            modifier = Modifier.size(120.dp)
        )

        Text("Welcome to Happy Kindergarten", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(30.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row {
            Button(onClick = {
                email = ""
                password = ""
            }) {
                Text("Cancel")
            }

            Spacer(modifier = Modifier.width(20.dp))

            Button(onClick = onLogin) {
                Text("Sign In")
            }
        }
    }
}

@Composable
fun MainMenuScreen(onLogout: () -> Unit, onParentClick: () -> Unit, onReportClick: () -> Unit, onRegisterClick: () -> Unit, onAttendanceClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(25.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "← Logout",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.clickable { onLogout() }
            )

            Spacer(modifier = Modifier.weight(1f))

            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier.size(75.dp)
            )
        }

        Spacer(modifier = Modifier.height(120.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row {
                MenuButton(R.drawable.register, "Register", onClick = onRegisterClick)
                Spacer(modifier = Modifier.width(50.dp))
                MenuButton(R.drawable.parent, "Parent", onClick = onParentClick)
            }

            Spacer(modifier = Modifier.height(50.dp))

            Row {
                MenuButton(R.drawable.attendance, "Attendance", onClick = onAttendanceClick)
                Spacer(modifier = Modifier.width(50.dp))
                MenuButton(R.drawable.report, "Report", onClick = onReportClick)
            }
        }
    }
}

@Composable
fun MenuButton(image: Int, text: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Image(
            painter = painterResource(image),
            contentDescription = text,
            modifier = Modifier.size(100.dp)
        )
        Text(text)
    }
}

@Composable
fun Header(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "←",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.clickable { onBack() }
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(title, style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.weight(1f))

        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "Logo",
            modifier = Modifier.size(70.dp)
        )
    }
}

@Composable
fun CustomChildHeader(title: String, onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(Color.Black, shape = CircleShape)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "←",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Image(
                painter = painterResource(R.drawable.register),
                contentDescription = "Register Badge",
                modifier = Modifier.size(45.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "Kindergarten Logo",
            modifier = Modifier.size(75.dp)
        )
    }
}
@Composable
fun ParentMenuScreen(
    onBack: () -> Unit,
    onAddParent: () -> Unit,
    onViewParent: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(25.dp)
    ) {
        Header(title = "Parent/Guardian", onBack = onBack)

        Spacer(modifier = Modifier.height(170.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = onAddParent, modifier = Modifier.size(130.dp)) {
                Text("Add\nParent")
            }

            Button(onClick = onViewParent, modifier = Modifier.size(130.dp)) {
                Text("View All\nParent")
            }
        }
    }
}

@Composable
fun ChildMenuScreen(
    onBack: () -> Unit,
    onAddStudent: () -> Unit,
    onViewStudents: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(25.dp)
    ) {
        CustomChildHeader(title = "Child Registration", onBack = onBack)

        Spacer(modifier = Modifier.height(180.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {

            Button(
                onClick = onAddStudent,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB2FF59)),
                modifier = Modifier.size(width = 160.dp, height = 150.dp)
            ) {
                Text(
                    text = "Add New\nStudent",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Button(
                onClick = onViewStudents,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF40C4FF)),
                modifier = Modifier.size(width = 160.dp, height = 150.dp)
            ) {
                Text(
                    text = "View All\nStudents",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
fun ChildFormScreen(
    student: StudentInfo?,
    onBack: () -> Unit,
    onSave: (StudentInfo) -> Unit
) {
    var fullName by remember { mutableStateOf(student?.fullName ?: "") }
    var nickname by remember { mutableStateOf(student?.nickname ?: "") }
    var gender by remember { mutableStateOf(student?.gender ?: "") }
    var dob by remember { mutableStateOf(student?.dob ?: "") }
    var address by remember { mutableStateOf(student?.address ?: "") }
    var religion by remember { mutableStateOf(student?.religion ?: "") }
    var nationality by remember { mutableStateOf(student?.nationality ?: "") }
    var allergies by remember { mutableStateOf(student?.allergies ?: "") }
    var chronicIllnesses by remember { mutableStateOf(student?.chronicIllnesses ?: "") }
    var className by remember { mutableStateOf(student?.className ?: "") }

    Column(
        modifier = Modifier.fillMaxSize().padding(25.dp)
    ) {
        CustomChildHeader(title = "Child Registration", onBack = onBack)

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))

                // Section 1: Personal Information Group
                Text(
                    text = "Personal information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                ParentTextField(fullName, { fullName = it }, "Full Name")
                ParentTextField(nickname, { nickname = it }, "Nickname")
                ParentTextField(gender, { gender = it }, "Gender")
                ParentTextField(dob, { dob = it }, "Date of Birth")
                ParentTextField(address, { address = it }, "Address")
                ParentTextField(religion, { religion = it }, "Religion")
                ParentTextField(nationality, { nationality = it }, "Nationality")

                Spacer(modifier = Modifier.height(20.dp))

                // Section 2: Health Information Group
                Text(
                    text = "Health",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                ParentTextField(allergies, { allergies = it }, "Allergies")
                ParentTextField(chronicIllnesses, { chronicIllnesses = it }, "Chronic Illnesses")

                Spacer(modifier = Modifier.height(20.dp))

                // Section 3: Class Allocation Group
                Text(
                    text = "Class",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                ParentTextField(className, { className = it }, "Class")

                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Bottom Action buttons layout matching "Happy Kindergarten (1).png"
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Cancel button
            Button(
                onClick = onBack,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB0BEC5)),
                modifier = Modifier.width(140.dp)
            ) {
                Text("Cancel", color = Color.Black)
            }

            // Register/Save button
            Button(
                onClick = {
                    onSave(
                        StudentInfo(
                            fullName = fullName,
                            nickname = nickname,
                            gender = gender,
                            dob = dob,
                            address = address,
                            religion = religion,
                            nationality = nationality,
                            allergies = allergies,
                            chronicIllnesses = chronicIllnesses,
                            className = className
                        )
                    )
                },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                modifier = Modifier.width(140.dp)
            ) {
                Text(if (student == null) "Register" else "Update", color = Color.White)
            }
        }
    }
}


@Composable
fun ViewChildScreen(
    studentList: List<StudentInfo>,
    onBack: () -> Unit,
    onUpdate: (Int) -> Unit,
    onDelete: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var deleteIndex by remember { mutableIntStateOf(-1) }

    if (showDialog) {
        DeleteConfirmationDialog(
            onConfirm = {
                if (deleteIndex >= 0) {
                    onDelete(deleteIndex)
                }
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(15.dp)
    ) {
        CustomChildHeader(title = "Child Registration", onBack = onBack)

        Text(
            text = "View All Students",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp),
            fontWeight = FontWeight.Bold
        )

        LazyColumn {
            itemsIndexed(studentList) { index, student ->
                StudentCard(
                    student = student,
                    onUpdate = { onUpdate(index) },
                    onDelete = {
                        deleteIndex = index
                        showDialog = true
                    }
                )
            }
        }
    }
}

@Composable
fun StudentCard(
    student: StudentInfo,
    onUpdate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(6.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Full Name: ${student.fullName}", fontWeight = FontWeight.Bold)
            Text("Nickname: ${student.nickname}")
            Text("Gender: ${student.gender} | Class: ${student.className}")
            Text("DOB: ${student.dob} | Nationality: ${student.nationality}")
            Text("Address: ${student.address}")
            Text("Religion: ${student.religion}")
            Text("Allergies: ${student.allergies}")
            Text("Chronic Illnesses: ${student.chronicIllnesses}")

            Spacer(modifier = Modifier.height(12.dp))

            Row {
                Button(onClick = onUpdate) {
                    Text("Update")
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(onClick = onDelete) {
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
fun ParentFormScreen(
    parent: ParentInfo?,
    onBack: () -> Unit,
    onSave: (ParentInfo) -> Unit
) {
    var childName by remember { mutableStateOf(parent?.childName ?: "") }
    var fatherName by remember { mutableStateOf(parent?.fatherName ?: "") }
    var fatherPhone by remember { mutableStateOf(parent?.fatherPhone ?: "") }
    var fatherAddress by remember { mutableStateOf(parent?.fatherAddress ?: "") }
    var motherName by remember { mutableStateOf(parent?.motherName ?: "") }
    var motherPhone by remember { mutableStateOf(parent?.motherPhone ?: "") }
    var motherAddress by remember { mutableStateOf(parent?.motherAddress ?: "") }
    var guardianName by remember { mutableStateOf(parent?.guardianName ?: "") }
    var guardianPhone by remember { mutableStateOf(parent?.guardianPhone ?: "") }
    var relation by remember { mutableStateOf(parent?.relation ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(25.dp)
    ) {
        Header(title = "Parent/Guardian", onBack = onBack)

        Text(
            text = if (parent == null) "Add Parent" else "Update Parent",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            item {
                ParentTextField(childName, { childName = it }, "Child name")
                ParentTextField(fatherName, { fatherName = it }, "Father name")
                ParentTextField(fatherPhone, { fatherPhone = it }, "Father phone", true)
                ParentTextField(fatherAddress, { fatherAddress = it }, "Father address")
                ParentTextField(motherName, { motherName = it }, "Mother name")
                ParentTextField(motherPhone, { motherPhone = it }, "Mother phone", true)
                ParentTextField(motherAddress, { motherAddress = it }, "Mother address")
                ParentTextField(guardianName, { guardianName = it }, "Guardian name")
                ParentTextField(guardianPhone, { guardianPhone = it }, "Guardian phone", true)
                ParentTextField(relation, { relation = it }, "Relation")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    onSave(
                        ParentInfo(
                            childName = childName,
                            fatherName = fatherName,
                            fatherPhone = fatherPhone,
                            fatherAddress = fatherAddress,
                            motherName = motherName,
                            motherPhone = motherPhone,
                            motherAddress = motherAddress,
                            guardianName = guardianName,
                            guardianPhone = guardianPhone,
                            relation = relation
                        )
                    )
                }
            ) {
                Text(if (parent == null) "Register" else "Update")
            }

            Button(onClick = onBack) {
                Text("Cancel")
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.width(100.dp)
            ) {
                Text("Delete", color = Color.White)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB0BEC5)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.width(100.dp)
            ) {
                Text("Cancel", color = Color.White)
            }
        },
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .border(3.dp, Color(0xFFE57373), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✕",
                        color = Color(0xFFE57373),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Light
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Are you sure?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Text(
                text = "Do you really want to delete these records? This process cannot be undone.",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun LogoutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF40C4FF)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.width(110.dp)
            ) {
                Text("Logout", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.width(110.dp)
            ) {
                Text("Cancel", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Text(
                text = "Are you sure you want to logout?",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            )
        },
        containerColor = Color(0xFFE0E0E0),
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun ParentTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    number: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Phone)
        else KeyboardOptions.Default,
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        singleLine = true
    )
}

@Composable
fun ViewParentScreen(
    parentList: List<ParentInfo>,
    onBack: () -> Unit,
    onUpdate: (Int) -> Unit,
    onDelete: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var deleteIndex by remember { mutableIntStateOf(-1) }

    if (showDialog) {
        DeleteConfirmationDialog(
            onConfirm = {
                if (deleteIndex >= 0) {
                    onDelete(deleteIndex)
                }
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(15.dp)
    ) {
        Header(title = "Parent/Guardian", onBack = onBack)

        Text(
            text = "View All Parent",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )

        LazyColumn {
            itemsIndexed(parentList) { index, parent ->
                ParentCard(
                    parent = parent,
                    onUpdate = { onUpdate(index) },
                    onDelete = {
                        deleteIndex = index
                        showDialog = true
                    }
                )
            }
        }
    }
}

@Composable
fun ParentCard(
    parent: ParentInfo,
    onUpdate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(6.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Child: ${parent.childName}")
            Text("Father: ${parent.fatherName}")
            Text("Father Phone: ${parent.fatherPhone}")
            Text("Father Address: ${parent.fatherAddress}")
            Text("Mother: ${parent.motherName}")
            Text("Mother Phone: ${parent.motherPhone}")
            Text("Mother Address: ${parent.motherAddress}")
            Text("Guardian: ${parent.guardianName}")
            Text("Guardian Phone: ${parent.guardianPhone}")
            Text("Relation: ${parent.relation}")

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Button(onClick = onUpdate) {
                    Text("Update")
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(onClick = onDelete) {
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
fun ReportScreen(
    reportList: List<StudentReport>,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Header(title = "Progress & Report", onBack = onBack)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = "",
            onValueChange = {},
            label = { Text("Search student") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ReportTable(reportList = reportList, editable = false)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onEdit,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Edit")
        }
    }
}

@Composable
fun EditReportScreen(
    reportList: MutableList<StudentReport>,
    onCancel: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Header(title = "Progress & Report", onBack = onCancel)

        Text(
            text = "Edit Report",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 10.dp)
        )

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ReportTable(reportList = reportList, editable = true)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(onClick = onCancel, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                Text("Cancel")
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(onClick = onDone) {
                Text("Done")
            }
        }
    }
}

@Composable
fun ReportTable(
    reportList: List<StudentReport>,
    editable: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth().border(1.dp, Color.Black)) {
        Row(modifier = Modifier.fillMaxWidth().background(Color.LightGray)) {
            TableCell("Name", Modifier.weight(1.5f), isHeader = true)
            TableCell("Literacy", Modifier.weight(1f), isHeader = true)
            TableCell("Numeracy", Modifier.weight(1f), isHeader = true)
            TableCell("SE", Modifier.weight(1f), isHeader = true)
            TableCell("PE", Modifier.weight(1f), isHeader = true)
        }

        reportList.forEachIndexed { index, student ->
            Row(modifier = Modifier.fillMaxWidth()) {
                TableCell(student.name, Modifier.weight(1.5f))

                if (editable && reportList is MutableList<StudentReport>) {
                    MarkField(student.literacy, Modifier.weight(1f)) {
                        reportList[index] = student.copy(literacy = it)
                    }
                    MarkField(student.numeracy, Modifier.weight(1f)) {
                        reportList[index] = student.copy(numeracy = it)
                    }
                    MarkField(student.socialSkills, Modifier.weight(1f)) {
                        reportList[index] = student.copy(socialSkills = it)
                    }
                    MarkField(student.physicalEducation, Modifier.weight(1f)) {
                        reportList[index] = student.copy(physicalEducation = it)
                    }
                } else {
                    TableCell(student.literacy, Modifier.weight(1f))
                    TableCell(student.numeracy, Modifier.weight(1f))
                    TableCell(student.socialSkills, Modifier.weight(1f))
                    TableCell(student.physicalEducation, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun TableCell(text: String, modifier: Modifier = Modifier, isHeader: Boolean = false) {
    Text(
        text = text,
        modifier = modifier
            .border(0.5.dp, Color.Black)
            .height(55.dp)
            .padding(4.dp)
            .wrapContentHeight(Alignment.CenterVertically),
        style = if (isHeader) MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
        else MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center
    )
}

@Composable
fun MarkField(
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .border(0.5.dp, Color.Black)
            .height(55.dp)
            .padding(4.dp),
        textStyle = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                innerTextField()
            }
        }
    )
}
@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    HappyKindergartenProjekTheme {
        LoginScreen(onLogin = {})
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    attendanceList: List<AttendanceRecord>,
    selectedDate: String,
    onDateChange: (String) -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val date = datePickerState.selectedDateMillis?.let {
                        SimpleDateFormat("d/M/yyyy", Locale.getDefault()).format(Date(it))
                    } ?: selectedDate
                    onDateChange(date)
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Header(title = "Attendance\n& Daily Record", onBack = onBack)

        Spacer(modifier = Modifier.height(30.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Date: ", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = selectedDate,
                onValueChange = { },
                readOnly = true,
                modifier = Modifier
                    .width(180.dp)
                    .height(50.dp)
                    .clickable { showDatePicker = true },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            painter = painterResource(android.R.drawable.ic_menu_my_calendar),
                            contentDescription = "Calendar"
                        )
                    }
                },
                singleLine = true,
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            AttendanceTable(attendanceList = attendanceList, editable = false)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onEdit,
            modifier = Modifier.align(Alignment.End),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text("Edit", color = Color.White, modifier = Modifier.padding(horizontal = 20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAttendanceScreen(
    attendanceList: MutableList<AttendanceRecord>,
    selectedDate: String,
    onDateChange: (String) -> Unit,
    onCancel: () -> Unit,
    onDone: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val date = datePickerState.selectedDateMillis?.let {
                        SimpleDateFormat("d/M/yyyy", Locale.getDefault()).format(Date(it))
                    } ?: selectedDate
                    onDateChange(date)
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Header(title = "Attendance\n& Daily Record", onBack = onCancel)

        Spacer(modifier = Modifier.height(30.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Date: ", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = selectedDate,
                onValueChange = { },
                readOnly = true,
                modifier = Modifier
                    .width(180.dp)
                    .height(50.dp)
                    .clickable { showDatePicker = true },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            painter = painterResource(android.R.drawable.ic_menu_my_calendar),
                            contentDescription = "Calendar"
                        )
                    }
                },
                singleLine = true,
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            )
        }

        Text(
            text = "Edit Mode",
            color = Color.Red,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            AttendanceTable(attendanceList = attendanceList, editable = true)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(onClick = onCancel, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                Text("Cancel")
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(onClick = onDone, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                Text("Done")
            }
        }
    }
}

@Composable
fun AttendanceTable(
    attendanceList: List<AttendanceRecord>,
    editable: Boolean
) {
    val headerBlue = Color(0xFF4FC3F7)
    Column(modifier = Modifier.fillMaxWidth().border(1.dp, Color.Black)) {
        // Table Header
        Row(modifier = Modifier.fillMaxWidth().background(headerBlue)) {
            TableCell("Name", Modifier.weight(1.2f), isHeader = true)
            TableCell("Status", Modifier.weight(0.8f), isHeader = true)
            TableCell("Remarks", Modifier.weight(1.5f), isHeader = true)
        }

        // Table Rows
        attendanceList.forEachIndexed { index, record ->
            Row(modifier = Modifier.fillMaxWidth()) {
                TableCell(record.name, Modifier.weight(1.2f))

                if (editable && attendanceList is MutableList<AttendanceRecord>) {
                    MarkField(record.status, Modifier.weight(0.8f)) {
                        attendanceList[index] = record.copy(status = it)
                    }
                    MarkField(record.remarks, Modifier.weight(1.5f)) {
                        attendanceList[index] = record.copy(remarks = it)
                    }
                } else {
                    TableCell(record.status, Modifier.weight(0.8f))
                    TableCell(record.remarks, Modifier.weight(1.5f))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AttendanceScreenPreview() {
    HappyKindergartenProjekTheme {
        AttendanceScreen(
            attendanceList = listOf(
                AttendanceRecord("Student A", "/", "-"),
                AttendanceRecord("Student B", "x", "Fever")
            ),
            selectedDate = "3/5/2026",
            onDateChange = {},
            onBack = {},
            onEdit = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EditAttendanceScreenPreview() {
    HappyKindergartenProjekTheme {
        val sampleList = remember {
            mutableStateListOf(
                AttendanceRecord("Student A", "/", "-"),
                AttendanceRecord("Student B", "x", "Fever")
            )
        }
        EditAttendanceScreen(
            attendanceList = sampleList,
            selectedDate = "3/5/2026",
            onDateChange = {},
            onCancel = {},
            onDone = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainMenuScreenPreview() {
    HappyKindergartenProjekTheme {
        MainMenuScreen(
            onLogout = {},
            onParentClick = {},
            {},
            {},
            {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ChildMenuScreenPreview() {
    HappyKindergartenProjekTheme {
        ChildMenuScreen(
            onBack = {},
            onAddStudent = {},
            onViewStudents = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ChildFormScreenPreview() {
    HappyKindergartenProjekTheme {
        ChildFormScreen(
            student = null,
            onBack = {},
            onSave = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ViewChildScreenPreview() {
    HappyKindergartenProjekTheme {
        ViewChildScreen(
            studentList = listOf(
                StudentInfo("Ali bin Abu", "Ali", "Male", "01/01/2020", "Kota Kinabalu", "Islam", "Malaysian", "None", "None", "Tadika Pintar")
            ),
            onBack = {},
            onUpdate = {},
            onDelete = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ParentMenuScreenPreview() {
    HappyKindergartenProjekTheme {
        ParentMenuScreen(
            onBack = {},
            onAddParent = {},
            onViewParent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ParentFormScreenPreview() {
    HappyKindergartenProjekTheme {
        ParentFormScreen(
            parent = null,
            onBack = {},
            onSave = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ViewParentScreenPreview() {
    HappyKindergartenProjekTheme {
        ViewParentScreen(
            parentList = listOf(
                ParentInfo(
                    childName = "Ali",
                    fatherName = "Abu",
                    fatherPhone = "0123456789",
                    fatherAddress = "Kota Kinabalu",
                    motherName = "Aina",
                    motherPhone = "0198765432",
                    motherAddress = "Kota Kinabalu",
                    guardianName = "Siti",
                    guardianPhone = "0112233445",
                    relation = "Aunt"
                )
            ),
            onBack = {},
            onUpdate = {},
            onDelete = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ReportScreenPreview() {
    HappyKindergartenProjekTheme {
        ReportScreen(
            reportList = listOf(
                StudentReport("Ali", "80", "75", "90", "85"),
                StudentReport("Mei", "88", "92", "84", "90")
            ),
            onBack = {},
            onEdit = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EditReportScreenPreview() {
    HappyKindergartenProjekTheme {
        val sampleReportList = remember {
            mutableStateListOf(
                StudentReport("Ali", "80", "75", "90", "85"),
                StudentReport("Mei", "88", "92", "84", "90")
            )
        }

        EditReportScreen(
            reportList = sampleReportList,
            onCancel = {},
            onDone = {}
        )
    }
}