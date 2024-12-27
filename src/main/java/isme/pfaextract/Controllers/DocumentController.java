package isme.pfaextract.Controllers;

import isme.pfaextract.Models.Document;
import isme.pfaextract.Models.User;
import isme.pfaextract.Repos.DocumentRepository;
import isme.pfaextract.Repos.UserRepository;
import isme.pfaextract.Services.OcrService;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentRepository documentRepository;
    private final OcrService ocrService;
    private final UserRepository userRepository;
    public DocumentController(DocumentRepository documentRepository, OcrService ocrService, UserRepository userRepository) {
        this.documentRepository = documentRepository;
        this.ocrService = ocrService;
        this.userRepository = userRepository;

    }
    @GetMapping("/userhistorique/{userId}")
    public ResponseEntity<List<Document>> getDocumentsByUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        List<Document> documents = documentRepository.findByUser(user);
        return ResponseEntity.ok(documents);
    }
    @PostMapping("/upload")
    public ResponseEntity<Document> uploadDocument(@RequestParam("file") MultipartFile file, @RequestParam("userId") Long userId) {
        try {
            // Extract text using OCR
            String extractedText = ocrService.extractText(file);

            // Find the user by ID
            User user = userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found"));
            // Create a new Document object and set the user
            Document document = new Document();
            document.setFileName(file.getOriginalFilename());
            document.setExtractedData(extractedText);
            document.setUploadDate(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
            document.setUser(user);

            // Save the document to the database
            documentRepository.save(document);

            return ResponseEntity.ok(document);
        } catch (IOException | TesseractException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocument(@PathVariable Long id) {
        return documentRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Document>> getAllDocuments() {
        return ResponseEntity.ok(documentRepository.findAll());
    }
}