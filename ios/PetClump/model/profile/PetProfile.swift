//
//  PetProfile.swift
//  PetClump
//
//  Created by YSH on 4/29/18.
//  Copyright © 2018 PetClump. All rights reserved.
//

import Foundation
import CoreLocation
import Firebase

class PetProfile: Profile, Deletable{
    private static let COLLECTION_NAME = "pets"
    public static var most_recent_pet: PetProfile?

    var bio: String     = ""
    var age: String     = ""
    var name: String    = ""
    var quiz: String    = ""
    var specie: String  = "Other"
    var ownerId: String = "error_id"
    var sequence: Int   = -1
    
    // Image
    private var url_map = [
        "main_url":"",
        "pet_view_1":"", "pet_view_2":"", "pet_view_3":"", "pet_view_4":"", "pet_view_5":"",
        "group_view_1":"", "group_view_2":"", "group_view_3":""
    ]
    
    init() {}
    convenience init(refObj: [String : Any]){
        self.init()
        fetchData(refObj: refObj)
    }
    convenience init(copyFromProfile: PetProfile){
        self.init()
        self.copy(FromProfile: copyFromProfile)
    }
    convenience init(uid: String, sequence: Int, completion: ( (PetProfile)->Void )?){
        self.init()
        self.ownerId = uid
        self.sequence = sequence
        download {
            guard completion != nil else { return }
            completion!(self)
        }
    }
    
    convenience init(id: String, completion: ((PetProfile)->Void)?){
        self.init()
        self.ownerId = String(id.dropLast())
        self.sequence = Int(String(id.last!))!
        download {
            guard completion != nil else { return }
            completion!(self)
        }
    }
    
    func copy(FromProfile: PetProfile){
        self.fetchData(refObj: FromProfile.generateDictionary())
    }
    
    private func fetchData(refObj: [String : Any]){
        self.age     = refObj["age"]  as? String ?? ""
        self.bio     = refObj["bio"]  as? String ?? ""
        self.quiz    = refObj["quiz"] as? String ?? ""
        self.name    = refObj["name"] as? String ?? ""
        self.specie  = refObj["spe"]  as? String ?? ""
        self.ownerId = refObj["owner_id"] as? String ?? ""
        self.sequence = refObj["sequence"] as? Int ?? -1
        self.url_map["main_url"] = refObj["main_url"] as? String ?? ""
        self.url_map["pet_view_1"] = refObj["pet_view_1"] as? String ?? ""
        self.url_map["pet_view_2"] = refObj["pet_view_2"] as? String ?? ""
        self.url_map["pet_view_3"] = refObj["pet_view_3"] as? String ?? ""
        self.url_map["pet_view_4"] = refObj["pet_view_4"] as? String ?? ""
        self.url_map["pet_view_5"] = refObj["pet_view_5"] as? String ?? ""
        self.url_map["group_view_1"] = refObj["group_view_1"] as? String ?? ""
        self.url_map["group_view_2"] = refObj["group_view_2"] as? String ?? ""
        self.url_map["group_view_3"] = refObj["group_view_3"] as? String ?? ""
    }

    func download(completion: (() -> Void)?) {
        // Opens document
        guard self.ownerId != "", self.sequence != -1 else {
            print("Attemp to download a pet profile without owner id and sequence number!")
            return
        }
        let generatedId = "\(self.ownerId)\(self.sequence)"
        let docRef =  Firestore.firestore().collection(PetProfile.COLLECTION_NAME).document(generatedId)
        docRef.getDocument { (document, error) in
            if let e = error{ print(e) }
            if let document = document, document.exists {
                // Unwraps data object
                let refObj = document.data()!
                
                // Gets user information
                self.fetchData(refObj: refObj)
            }
            guard (completion != nil) else { return }
            completion!()
        }
    }
    
    func generateDictionary() -> [String : Any] {
        return [
            "bio":  bio,
            "age":  age,
            "spe":  specie,
            "name": name,
            "quiz": quiz,
            "owner_id": ownerId,
            "sequence": sequence,
            "main_url": url_map["main_url"] ?? "",
            "pet_view_1": url_map["pet_view_1"] ?? "",
            "pet_view_2": url_map["pet_view_2"] ?? "",
            "pet_view_3": url_map["pet_view_3"] ?? "",
            "pet_view_4": url_map["pet_view_4"] ?? "",
            "pet_view_5": url_map["pet_view_5"] ?? "",
            "group_view_1": url_map["group_view_1"] ?? "",
            "group_view_2": url_map["group_view_2"] ?? "",
            "group_view_3": url_map["group_view_3"] ?? ""
        ]
    }
    
    enum PetPhotoUrlKey {
        case main, pet1, pet2, pet3, pet4, pet5, group1, group2, group3
    }
    
    func getPhotoUrl(key: PetPhotoUrlKey) -> String{
        switch key {
        case .main: return url_map["main_url"] ?? ""
        case .pet1: return url_map["pet_view_1"] ?? ""
        case .pet2: return url_map["pet_view_2"] ?? ""
        case .pet3: return url_map["pet_view_3"] ?? ""
        case .pet4: return url_map["pet_view_4"] ?? ""
        case .pet5: return url_map["pet_view_5"] ?? ""
        case .group1: return url_map["group_view_1"] ?? ""
        case .group2: return url_map["group_view_2"] ?? ""
        case .group3: return url_map["group_view_3"] ?? ""
        }
    }
    
    func getPetPhotoUrls() -> [String] {
        return getPhotoUrls(isPet: true)
    }
    
    func getGroupPhotoUrls() -> [String] {
        return getPhotoUrls(isPet: false)
    }
    
    private func getPhotoUrls(isPet: Bool) -> [String] {
        var urls: [String] = []
        let keys = Array(url_map.keys).sorted()
        for key in keys {
            let value = url_map[key] ?? ""
            if value == "" { continue }
            if isPet {
                if isGroupPhotoKey(keyString: key){
                    continue
                }
                urls.append(value)
            } else {
                if isGroupPhotoKey(keyString: key){
                    urls.append(value)
                }
            }
        }
        return urls
    }
    
    private func isGroupPhotoKey(keyString: String) -> Bool {
        return keyString.contains(toSearch: "group")
    }
    func setPhotoUrl(key: PetPhotoUrlKey, url: String){
        switch key {
        case .main: url_map["main_url"] = url
        case .pet1: url_map["pet_view_1"] = url
        case .pet2: url_map["pet_view_2"] = url
        case .pet3: url_map["pet_view_3"] = url
        case .pet4: url_map["pet_view_4"] = url
        case .pet5: url_map["pet_view_5"] = url
        case .group1: url_map["group_view_1"] = url
        case .group2: url_map["group_view_2"] = url
        case .group3: url_map["group_view_3"] = url
        }
    }
    
    func upload(vc: QuickAlert?, completion: (() -> Void)?) {
        guard let _ = Auth.auth().currentUser?.uid else {
            guard vc != nil else { return }
            vc!.makeAlert(message: "User is not signed in!")
            return
        }
        let docRef =  Firestore.firestore().collection(PetProfile.COLLECTION_NAME).document(getId())
        docRef.setData(self.generateDictionary()) { (err: Error?) in
            if let err = err{
                guard vc != nil else { return }
                vc!.makeAlert(message: "Upload failed, reason:" + err.localizedDescription)
            }
            print("Uploaded successfully for pet " + self.getId())
            guard (completion != nil) else { return }
            completion!()
        }
    }
    
    func delete(){
        guard let _ = Auth.auth().currentUser?.uid else { return }
        let petId = getId()
        // TODO delete photos
        Firestore.firestore().collection(PetProfile.COLLECTION_NAME).document(petId).delete(completion: { (error) in
            if let err = error {
                print(err)
            } else {
                print("Pet deleted :(\(petId)")
            }
        })
    }
    
    func getId() -> String{
        return "\(ownerId)\(sequence)"
    }
    
    
    static func tagToPhotoKey(tag: Int) -> PetProfile.PetPhotoUrlKey{
        switch tag {
        case 0: return  .main
        case 1: return  .pet1
        case 2: return  .pet2
        case 3: return  .pet3
        case 4: return  .pet4
        case 5: return  .pet5
        case 6: return  .group1
        case 7: return  .group2
        case 8: return  .group3
        default: return .main
        }
    }
    
    func deleteImage(photoKey: PetProfile.PetPhotoUrlKey){
        let url = self.getPhotoUrl(key: photoKey)
        if url == "" { return }
        self.setPhotoUrl(key: photoKey, url: "")
        let groups = url.capturedGroups(withRegex: ".*%2[fF](.*)\\?.*")
        print("will delete photo: image/\(groups[0])")
        Storage.storage().reference(withPath: "image/\(groups[0])").delete { (error) in
            if let err = error {
                print("error deleting photo: " + err.localizedDescription)
            }
        }
    }
    
    func generateChatRoomId(otherProfile: PetProfile) -> String{
        let myId = self.getId()
        let friendId = otherProfile.getId()
        return myId > friendId ? "\(myId)\(friendId)" : "\(friendId)\(myId)"
    }
}
