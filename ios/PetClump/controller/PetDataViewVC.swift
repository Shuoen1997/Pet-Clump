//
//  PetDataViewVC.swift
//  PetClump
//
//  Created by YSH on 4/27/18.
//  Copyright © 2018 PetClump. All rights reserved.
//

import UIKit
import Firebase

class PetDataViewVC: UIViewController, UIImagePickerControllerDelegate, UINavigationControllerDelegate, ProfileDownloader{
    //Title Labels
    @IBOutlet weak var nameTitleLabel:          UILabel!
    @IBOutlet weak var petAndOwnerTitleLabel:   UILabel!
    @IBOutlet weak var infoTitleLabel:          UILabel!
    @IBOutlet weak var petNameTitleLabel:       UILabel!
    @IBOutlet weak var petSpeciesLabel:         UILabel!
    @IBOutlet weak var petAgeTitleLabel:        UILabel!
    @IBOutlet weak var petBioTitleLabel:        UILabel!
    @IBOutlet weak var bioRemainingLabel:       UILabel!

    //Information display
    @IBOutlet weak var petNameTextField:    UITextField!
    @IBOutlet weak var petSpeciesTextField: UITextField!
    @IBOutlet weak var petAgeTextField:     UITextField!
    @IBOutlet weak var petBioTextView:      UITextView!
    //Pet pictures display
    @IBOutlet weak var bigPetPicture0:    UIImageView!
    @IBOutlet weak var smallPetPicture1: UIImageView!
    @IBOutlet weak var smallPetPicture2: UIImageView!
    @IBOutlet weak var smallPetPicture3: UIImageView!
    @IBOutlet weak var smallPetPicture4: UIImageView!
    @IBOutlet weak var smallPetPicture5: UIImageView!
    //Pet and Owner Pictures display
    @IBOutlet weak var petAndOwnerPic6: UIImageView!
    @IBOutlet weak var petAndOwnerPic7: UIImageView!
    @IBOutlet weak var petAndOwnerPic8: UIImageView!

    // Buttons
    @IBOutlet weak var quizButton: UIButton!
    @IBOutlet weak var deleteButton: UIButton!
    @IBOutlet weak var saveButton: UIBarButtonItem!
    @IBOutlet weak var exitButton: UIBarButtonItem!
    
    var petProfile:     PetProfile?
    var speciePicker:   UIPickerView?
    var ageInputDelegate:   UITextFieldDelegate?
    var nameInputDelegate:  UITextFieldDelegate?
    var speciePickerDelegate: SpeciePicker?
    var remainingBioDelegate: UITextViewDelegate?
    
    //variable for the image tag
    var imageTag = -1
    
    func uploadImageToFirebaseStorage(data: NSData, tag: Int){
        //upload to firebase
        let fileName = NSUUID.init().uuidString + ".png"
        let storageRef = Storage.storage().reference(withPath: "image/\(fileName)")
        let uploadMetaData = StorageMetadata()
        uploadMetaData.contentType =  "image/png"
        storageRef.putData(data as Data, metadata: uploadMetaData) {
            (metadata, error) in if (error != nil) {
                print("I received an error! \(String(describing: error?.localizedDescription))")
            } else {
                storageRef.downloadURL(completion: { (url, error) in
                    if error != nil {
                        print("There is an error when uploading: \(error!)")
                    } else {
                        print("\(url!) for \(tag)")
                        switch tag {
                        case 0: self.petProfile!.url_map["main_profile_url"] = "\(url!)"
                        case 1: self.petProfile!.url_map["pet_profile_url_1"] = "\(url!)"
                        case 2: self.petProfile!.url_map["pet_profile_url_2"] = "\(url!)"
                        case 3: self.petProfile!.url_map["pet_profile_url_3"] = "\(url!)"
                        case 4: self.petProfile!.url_map["pet_profile_url_4"] = "\(url!)"
                        case 5: self.petProfile!.url_map["pet_profile_url_5"] = "\(url!)"
                        case 6: self.petProfile!.url_map["group_profile_url_1"] = "\(url!)"
                        case 7: self.petProfile!.url_map["group_profile_url_2"] = "\(url!)"
                        case 8: self.petProfile!.url_map["group_profile_url_3"] = "\(url!)"
                        default: break
                        }
                    }
                })
            }
        }
    }
    
    func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
        dismiss(animated: true, completion: nil)
    }
    func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [String : Any]) {
        guard let _: String = info[UIImagePickerControllerMediaType] as? String else {
            dismiss(animated: true, completion: nil)
            return
        }
        if let originalImage = info[UIImagePickerControllerOriginalImage] as? UIImage, let imageData = UIImageJPEGRepresentation(originalImage, 0.8) {
            uploadImageToFirebaseStorage(data: imageData as NSData, tag: self.imageTag)
            switch self.imageTag {
            case 0: self.bigPetPicture0.image   = originalImage
            case 1: self.smallPetPicture1.image = originalImage
            case 2: self.smallPetPicture2.image = originalImage
            case 3: self.smallPetPicture3.image = originalImage
            case 4: self.smallPetPicture4.image = originalImage
            case 5: self.smallPetPicture5.image = originalImage
            case 6: self.petAndOwnerPic6.image  = originalImage
            case 7: self.petAndOwnerPic7.image  = originalImage
            case 8: self.petAndOwnerPic8.image  = originalImage
            default: break
            }
        }
        dismiss(animated: true, completion: nil)
        
    }
    
    @IBAction func tapToUploadImage(_ sender: UITapGestureRecognizer) {
        let imagePicker = UIImagePickerController()
        imagePicker.sourceType = .photoLibrary
        let image = sender.view
        self.imageTag = image!.tag
        print("\(imageTag)")
        imagePicker.delegate = self
        present(imagePicker, animated: true, completion: nil)
    }
    
    
    
    override func viewDidLoad() {
        super.viewDidLoad()
        if let _ = Auth.auth().currentUser?.uid {
            self.setupUI()
            setupDelegate()
        } else {
            self.dismiss(animated: true, completion: nil)
        }
    }
    
    override func viewWillAppear(_ animated: Bool) {
        guard let uid = Auth.auth().currentUser?.uid else { return }
        petProfile!.download(uid: uid, completion: self)
    }
    
    // Pre-fill text fields when pet info is downloaded from Firebase
    func didCompleteDownload() {
        self.petBioTextView.text        = petProfile!.bio
        self.petAgeTextField.text       = petProfile!.age
        self.petNameTextField.text      = petProfile!.name
        self.bioRemainingLabel.text     = "\(petProfile!.bio.count)/500"
        self.petSpeciesTextField.text   = petProfile!.specie
        self.quizButton.titleLabel!.text = NSLocalizedString("Start Quiz (\(petProfile!.quiz.count)/100)", comment: "This is the button that takes the user to quiz view. It shows how many quiz this user has complete for this particular pet")
        self.deleteButton.addTarget(self, action: #selector(deletePet), for: .touchUpInside)
        self.bigPetPicture0.load(url: self.petProfile!.url_map["main_profile_url"] ?? "")
        self.smallPetPicture1.load(url: self.petProfile!.url_map["pet_profile_url_1"] ?? "")
        self.smallPetPicture2.load(url: self.petProfile!.url_map["pet_profile_url_2"] ?? "")
        self.smallPetPicture3.load(url: self.petProfile!.url_map["pet_profile_url_3"] ?? "")
        self.smallPetPicture4.load(url: self.petProfile!.url_map["pet_profile_url_4"] ?? "")
        self.smallPetPicture5.load(url: self.petProfile!.url_map["pet_profile_url_5"] ?? "")
        self.petAndOwnerPic6.load(url: self.petProfile!.url_map["group_profile_url_1"] ?? "")
        self.petAndOwnerPic7.load(url: self.petProfile!.url_map["group_profile_url_2"] ?? "")
        self.petAndOwnerPic8.load(url: self.petProfile!.url_map["group_profile_url_3"] ?? "")
        
        
    }
    @objc private func deletePet(){
        confirmBeforeDelete(title: NSLocalizedString("Delete this pet?", comment: "This is the title on an alert when user clicks delete pet"), message: NSLocalizedString("Are you sure you want to delete this pet? This action cannot be undone.", comment: "This is the message when the user clicks delete pet"), toDelete: petProfile!)
    }
    
    private func setupUI(){
        self.nameTitleLabel.text        = NSLocalizedString("Pet Name", comment: "This is the title for specifying the name of the pet")
        self.infoTitleLabel.text        = NSLocalizedString("INFO", comment: "This is the title for the section of the pet information")
        self.petSpeciesLabel.text       = NSLocalizedString("Species", comment: "This is the title for specifying the species of the pet")
        self.petAgeTitleLabel.text      = NSLocalizedString("Age", comment: "This is the title for specifying the age of the pet")
        self.petBioTitleLabel.text      = NSLocalizedString("Bio", comment: "This is the title for specifying the Bio of the pet")
        self.petNameTitleLabel.text     = NSLocalizedString("Name", comment: "This is the title for specifying the name of the pet in the pet info section")
        self.petAndOwnerTitleLabel.text = NSLocalizedString("Pet And I", comment: "This is the title for the picture section of the pet and owner")
    }
    
    private func setupDelegate(){
        // Specie picker
        speciePicker = UIPickerView()
        speciePickerDelegate = SpeciePicker(textField: petSpeciesTextField)
        speciePicker!.delegate = speciePickerDelegate
        speciePicker!.dataSource = speciePickerDelegate
        petSpeciesTextField.delegate = speciePickerDelegate
        petSpeciesTextField.inputView = speciePicker
        
        // Text fields and textview delegates
        nameInputDelegate = LimitTextFieldInput(count: 20)
        ageInputDelegate  = LimitTextFieldInput(count: 50)
        remainingBioDelegate = LimitTextViewInput(count: 500, remainingLable: bioRemainingLabel)
        petBioTextView.makeTextField(delegate: remainingBioDelegate!)
        petNameTextField.delegate = nameInputDelegate
        petAgeTextField.delegate  = ageInputDelegate
    }
    
    
    @IBAction func tapSave(_ sender: Any) {
        if let uid = Auth.auth().currentUser?.uid{
            petProfile!.name = petNameTextField.text!
            petProfile!.bio  = petBioTextView.text!
            petProfile!.age  = petAgeTextField.text!
            petProfile!.specie = petSpeciesTextField.text!
            petProfile!.ownerId = uid
            petProfile!.upload(vc: self, completion: nil)
            self.makeAlert(message: NSLocalizedString("Your pet information is saved!", comment: "This is a alter message that shows up and the user tap save on the pet information viewing page."))
        }
        
    }
    
    @IBAction func tapQuiz(_ sender: Any){
        if petProfile!.quiz.count == 100 {
            makeAlert(message: NSLocalizedString("You have complete all the questions!", comment: "This is an alert message when the user clicks the Start Quiz button but have finished all 100 questions"))
        }
        let storyBoard: UIStoryboard = UIStoryboard(name: "Main", bundle: nil)
        let qvc = storyBoard.instantiateViewController(withIdentifier: "QuizVC") as! QuizVC
        qvc.petProfile = PetProfile()
        qvc.petProfile!.sequence = self.petProfile!.sequence
        self.present(qvc, animated: true, completion: nil)
    }
    
    @IBAction func tapExit(_ sender: Any) {
        self.dismiss(animated: true, completion: nil)
    }
}
