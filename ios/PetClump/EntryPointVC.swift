//
//  ViewController.swift
//  PetClump
//
//  Created by YSH on 4/11/18.
//  Copyright © 2018 PetClump. All rights reserved.
//

import UIKit
import Firebase
import GoogleSignIn
import FacebookLogin
import FacebookCore
import FBSDKCoreKit
import FBSDKShareKit
import FBSDKLoginKit

class EntryPointVC: UIViewController, QuickAlert, GIDSignInUIDelegate, FBSDKLoginButtonDelegate, GIDSignInDelegate{

    
    @IBOutlet weak var signInButtonGoogle: GIDSignInButton!
    @IBOutlet weak var signOutButtonGoogle: UIButton!
    @IBOutlet weak var facebookLoginButtonView: UIView!
    @IBOutlet weak var uidLabel: UILabel!
    var signInButtonFacebook: FBSDKLoginButton?
    
    let debugMode = true
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        // Handels Google sign-in
        GIDSignIn.sharedInstance().clientID = FirebaseApp.app()?.options.clientID
        GIDSignIn.sharedInstance().delegate = self
        GIDSignIn.sharedInstance().uiDelegate = self
        
        // Creates Facebook sign-in
        signInButtonFacebook = FBSDKLoginButton()
        if let btn = signInButtonFacebook{
            btn.delegate = self
            btn.center = facebookLoginButtonView.center
            facebookLoginButtonView.backgroundColor = .clear
            view.addSubview(btn)
        }
        transitionLoginStateUI()
    }

    /**
     * This method sets up UI after login state change
     */
    func transitionLoginStateUI(){
        if let user = Auth.auth().currentUser {
            view.backgroundColor = .green
            uidLabel.text = user.uid
        } else {
            view.backgroundColor = .yellow
            uidLabel.text = "You are not logged in"
        }
    }
    
    /**
     * This action signs out any Firebase authenticated user if signed in, then alerts the result.
     * - Parameter sender: the action sender
     */
    @IBAction func topSignOut(_ sender: Any) {
        // Signs out the user
        var message = "You have already signed out"
        if let user = Auth.auth().currentUser{
            do {
                try Auth.auth().signOut()
                message = "You have signed out as " + user.uid
            } catch let signOutError as NSError {
                message = String.init(format: "Error signing out: %@", signOutError)
            }
        }
        // Alterts result
        self.transitionLoginStateUI()
        self.makeAlert(message: message)
    }
    
    /**
     * This method checks if Firebase is authenicated by a user account.
     * - Returns: ture if login
     */
    func isSignedIn(alertUid: Bool) -> Bool {
        // Checks if firebase has alreay authenticated a user
        if let user = Auth.auth().currentUser{
            if alertUid {
                self.makeAlert(message: "You have already signed in as " + user.uid)
            }
            return true
        }
        return false
    }
    
    func isSignedIn() -> Bool{
        return isSignedIn(alertUid: false)
    }
    
    /**
     * This method authenticates Firebase with the user credential.
     */
    func authenticateFirebase(credential: AuthCredential, withError error: Error? ){
        // Error checking
        if isSignedIn(alertUid: false){
            self.transitionLoginStateUI()
            return
            
        }
        if let error = error {
            print(error.localizedDescription)
            return
        }
        
        var message = "Sign in failed"
        Auth.auth().signIn(with: credential) { (user, error) in
            if let error = error {
                message = error.localizedDescription
            }
            if let uid = user?.uid{
                message = "You have logged in as " + uid
            }
            self.transitionLoginStateUI()
            self.makeAlert(message: message)
        }
    }
    
    /**
     * This method make a alter with OK button.
     * - Parameter message: A string mess to show in the alter
     */
    func makeAlert(message: String){
        // Make alert
        let alert = UIAlertController(title: "Message", message: message, preferredStyle: UIAlertControllerStyle.alert)
        // add an action (button)
        alert.addAction(UIAlertAction(title: "OK", style: UIAlertActionStyle.default, handler: nil))
        // show the alert
        self.present(alert, animated: true, completion: nil)
    }
    
    // ===============
    //     Google
    // ===============
    @available(iOS 9.0, *)
    func application(_ application: UIApplication, open url: URL, options: [UIApplicationOpenURLOptionsKey : Any])
        -> Bool {
            return GIDSignIn.sharedInstance().handle(url,
                                                     sourceApplication:options[UIApplicationOpenURLOptionsKey.sourceApplication] as? String,
                                                     annotation: [:])
    }
    
    /**
     * This method implements the protocal when the user clicks Google logout button.
     */
    func sign(_ signIn: GIDSignIn!, didSignInFor user: GIDGoogleUser!, withError error: Error?) {
        
        // Authenticates Firebase with the Google user
        guard let authentication = user.authentication else { return }
        let credential = GoogleAuthProvider.credential(withIDToken: authentication.idToken,
                                                       accessToken: authentication.accessToken)
        authenticateFirebase(credential: credential, withError: error)
    }
    
    func sign(_ signIn: GIDSignIn!, didDisconnectWith user: GIDGoogleUser!, withError error: Error!) {
        // Perform any operations when the user disconnects from app here.
        // ...
    }
    
    
    // ===============
    //    Facebook
    // ===============
    /**
     * This method implements the protocal when the user clicks Facebook loggin button.
     */
    func loginButton(_ loginButton: FBSDKLoginButton!, didCompleteWith result: FBSDKLoginManagerLoginResult!, error: Error!) {
        // Authenticates Firebase with the Facebook user
        guard (FBSDKAccessToken.current() != nil) else { return }
        let credential = FacebookAuthProvider.credential(withAccessToken: FBSDKAccessToken.current().tokenString)
        authenticateFirebase(credential: credential, withError: error)
    }
    
    /**
     * This method implements the protocal when the user clicks Facebook logout button.
     */
    func loginButtonDidLogOut(_ loginButton: FBSDKLoginButton!) {
        var message = "You have already signed out"
        if let uid = Auth.auth().currentUser?.uid{
            message = "You have logged out Facebook as " + uid
        }
        makeAlert(message: message);
        transitionLoginStateUI()
    }
}
