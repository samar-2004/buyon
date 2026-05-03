import com.google.firebase.auth.FirebaseAuth;
class TestFile {
  public static void main(String[] args) {
    FirebaseAuth.getInstance().getFirebaseAuthSettings().forceRecaptchaFlowForTesting(true);
    FirebaseAuth.getInstance().getFirebaseAuthSettings().setAppVerificationDisabledForTesting(true);
  }
}
