# Buyon

**Buyon** is a native Android e‑commerce / online shopping application. It lets
customers browse a catalog of products, add items to a cart and wishlist, place
orders and pay, while a separate **admin** experience lets store operators manage
the catalog, view orders and manage users.

The app is built in **Java** on top of **Firebase** (Authentication + Cloud
Firestore) for its backend, with **Cloudinary** used for product image hosting.

---

## Features

### Customer
- **Authentication** – email/password sign‑in and **Google Sign‑In** (Firebase Auth).
- **Onboarding & splash** flow for first‑time users.
- **Home & search** – browse products by category and search the catalog.
- **Product detail** – product images, pricing and details.
- **Cart** – add/remove items, adjust quantities.
- **Wishlist** – save products for later.
- **Checkout & payment** – place orders and choose a payment method.
- **Orders** – order confirmation and order history.
- **Profile** – manage the signed‑in user’s profile.

### Admin
- **Admin dashboard** with separate navigation and theme.
- **Product management** – add and manage products (with image upload).
- **Order management** – view and manage customer orders.
- **User management** – view and manage registered users.

Access is **role‑based** (`user` / `admin`), enforced both in the app and in the
Firestore security rules (`firestore.rules`).

---

## Tech stack

| Area            | Technology |
|-----------------|------------|
| Language        | Java 11 |
| Platform        | Android (min SDK 29, target/compile SDK 36) |
| Backend         | Firebase Authentication, Cloud Firestore |
| Image hosting   | Cloudinary (uploaded via Retrofit/OkHttp) |
| Sign‑in         | Google Play Services Auth |
| UI              | Material Components, AndroidX Navigation, View Binding, ConstraintLayout, RecyclerView, ViewPager2, SwipeRefreshLayout |
| Image loading   | Glide |
| Animation / UX  | Lottie (animations), Shimmer (loading placeholders) |
| Architecture    | MVVM (ViewModel + LiveData) with a Repository pattern and manual dependency injection |
| Build           | Gradle (Kotlin DSL) with version catalog (`gradle/libs.versions.toml`) |

---

## Project structure

```
app/src/main/java/com/buyon/
├── app/          App entry points (BuyonApplication, MainActivity, AppContainer, BaseActivity)
├── core/         DI wiring, utilities, resource helpers
├── domain/       Business layer
│   ├── model/        Product, Category, CartItem, Order, OrderLine, UserProfile, PaymentMethod, PaymentStatus
│   ├── repository/   Repository interfaces (Auth, Product, Cart, Order, Payment, Wishlist, Admin, …)
│   └── callback/     Async result callbacks
├── data/         Data layer
│   ├── firebase/     Firestore-backed repository implementations + mappers
│   ├── auth/         Authentication data sources
│   └── prefs/        Local preferences
├── network/      Cloudinary API client (Retrofit) + responses
├── repository/   ImageRepository (image upload)
└── ui/           Feature screens (fragments + ViewModels)
    ├── auth, splash, onboarding, home, search, product, cart, wishlist,
    │   checkout, payment, orders, profile, pricing, upload, adapters, util
    └── admin/        Admin dashboard, products, orders, users
```

The app uses a clean layering approach: **UI** (Fragments/ViewModels) depends on
**domain** repository interfaces, which are implemented in the **data** layer
against Firestore. Dependencies are assembled manually through
`AppContainer` / `AppDependencies` and exposed from `BuyonApplication`.

Two main entry activities are defined in the manifest:
- `MainActivity` – the customer app (launcher activity).
- `AdminActivity` – the admin dashboard.
- `WishlistActivity` – standalone wishlist screen.

Navigation is driven by the AndroidX Navigation component with two graphs:
`nav_graph.xml` (customer) and `admin_nav_graph.xml` (admin).

---

## Getting started

### Prerequisites
- Android Studio (latest stable) with Android SDK 36.
- A Firebase project with **Authentication** (Email/Password + Google) and
  **Cloud Firestore** enabled.
- A Cloudinary account with an unsigned upload preset (for product images).

### Configuration

1. **Firebase** – add your `google-services.json` to the `app/` directory.
   *(A file is present in this repo; replace it with your own project's config
   if you are deploying your own backend.)*

2. **Cloudinary** – the build reads two values from `local.properties`
   (or environment variables) and exposes them via `BuildConfig`:

   ```properties
   # local.properties
   sdk.dir=/path/to/Android/sdk
   CLOUDINARY_CLOUD_NAME=your_cloud_name
   CLOUDINARY_UPLOAD_PRESET=your_unsigned_upload_preset
   ```

3. **Firestore security rules** – deploy the rules in `firestore.rules`
   (and `app/src/main/java/com/buyon/firebase/storage.rules` if you use Storage)
   to your Firebase project.

### Build & run

```bash
# Build a debug APK
./gradlew assembleDebug

# Install on a connected device / emulator
./gradlew installDebug
```

Or open the project in Android Studio and run the `app` configuration.

---

## Notes

- `local.properties` and `google-services.json` contain environment‑specific
  configuration and should not be committed with real credentials.
- The default application id / namespace is `com.buyon.app`.
