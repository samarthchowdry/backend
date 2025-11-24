package com.studentdetails.details.Service.ServiceImpl;

import com.studentdetails.details.Repository.StudentRepository;
import com.studentdetails.details.Service.AdminCommunicationService;
import com.studentdetails.details.Service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminCommunicationServiceImpl implements AdminCommunicationService {

    private final StudentRepository studentRepository;
    private final EmailService emailService;

    @Override
    public int sendBroadcastEmail(String subject, String message) {
        var emails = studentRepository.findAll().stream()
                .map(student -> student.getEmail())
                .filter(email -> email != null && !email.isBlank())
                .toList();

        emails.forEach(email -> emailService.sendEmail(email, subject, message));
        return emails.size();
    }
}


