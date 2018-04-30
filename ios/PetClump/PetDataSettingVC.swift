//
//  PetDataSettingVC.swift
//  PetClump
//
//  Created by YSH on 4/27/18.
//  Copyright © 2018 PetClump. All rights reserved.
//

import UIKit


class PetDataSettingVC: UIViewController{
    
    private var profile: PetProfile = PetProfile()
    private var bioLimitDelegate: LimitTextFieldInput?
    private var nameDelegate: LimitTextFieldInput?
    private var ageDelegate: LimitTextFieldInput?
    private var specitDelegate: LimitTextFieldInput?
    
    //View UI
    @IBOutlet weak var petSettingNavigationBar: UINavigationBar!
    @IBOutlet weak var petName: UILabel!
    @IBOutlet weak var petSpecies: UILabel!
    @IBOutlet weak var petAge: UILabel!
    @IBOutlet weak var petBio: UILabel!
    //Controller UI
    @IBOutlet weak var petSaveButton: UIBarButtonItem!
    @IBOutlet weak var petNameTextField: UITextField!
    @IBOutlet weak var petSpeciesTextField: UITextField!
    @IBOutlet weak var petAgeTextField: UITextField!
    @IBOutlet weak var petBioTextField: UITextView!
    
    
    
    @IBAction func tapExit(_ sender: Any) {
        self.dismiss(animated: true, completion: nil)
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
    }
    
    func setupUI(){
        
    }
    
    func onComplete() {
        
    }
    
    @IBAction func tapExit(_ sender: Any) {
        self.dismiss(animated: true, completion: nil)
    }
    
    //when pressing the save button
    @IBAction func tapToUploadPetInfo(_ sender: Any) {
    }
}
