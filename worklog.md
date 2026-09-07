# WASLNI (وصلني) — Project Worklog

> Shared work log for all agents working on the WASLNI delivery navigation app.
> Append-only. Each new section starts with `---`.

---
Task ID: phase-1
Agent: main
Task: Phase 1 — Planning: Produce complete reference documentation before any code.

Work Log:
- Read and analyzed the full PRD (100 sections) and execution order from the user.
- Asked 8 clarifying questions; user pinned: App=WASLNI, Package=com.waslni.driver, Min SDK=24, Map=Mapbox, Auth=Username+Password, Backend=FastAPI+PostgreSQL (parallel with Android), Architecture=Clean+MVVM, Code delivery=In-chat, First phase=Planning.
- Created /home/z/my-project/waslni/docs/ directory.
- Wrote 01-PRD.md — Product Requirements Document with MVP scope, use cases, user flows, UI specs, error handling, acceptance criteria, risks.
- Wrote 02-architecture.md — Clean Architecture + MVVM, Android stack, Backend stack, Mapbox provider abstraction, sync architecture, security, battery strategy.
- Wrote 03-database.md — PostgreSQL schema (7 tables: users, customers, deliveries, refresh_tokens, audit_logs, idempotency_keys) + Room schema (3 entities) + DAOs + SQLAlchemy models + Alembic plan.
- Wrote 04-api-contract.md — Full OpenAPI-style contract: 4 auth endpoints, 5 customer endpoints, 4 delivery endpoints, 1 sync endpoint. Unified error model, idempotency, conflict resolution, rate limiting, Pydantic schemas, Retrofit interfaces.
- Wrote 05-project-structure.md — Monorepo layout, Android package structure (core/data/domain/di/presentation), backend layout, Git strategy (Conventional Commits + branching), CI/CD pipelines (GitHub Actions), versioning, code review guidelines.
- Wrote 06-build-phases.md — 32-phase roadmap from Planning to MVP Launch with dependencies, acceptance criteria, timeline estimate, milestones.

Stage Summary:
- Phase 1 (Planning) complete.
- 6 reference documents produced in /home/z/my-project/waslni/docs/:
  - 01-PRD.md
  - 02-architecture.md
  - 03-database.md
  - 04-api-contract.md
  - 05-project-structure.md
  - 06-build-phases.md
- Next: Phase 2 (Android Foundation) — create Android project skeleton with Compose + Hilt + Navigation + Theme + RTL.
- Critical decisions locked: Mapbox for maps, FastAPI parallel with Android, Offline-first with Room as source of read, latest-write-wins for customer conflicts, Idempotency-Key for delivery completion.

---
Task ID: phase-2
Agent: main
Task: Phase 2 — Android Foundation: Create Android project skeleton with Compose + Hilt + Navigation + Theme + RTL + placeholder screens.

Work Log:
- Created /home/z/my-project/waslni/android/ directory tree.
- Wrote gradle/libs.versions.toml — version catalog with all dependencies (Compose BOM, Hilt, Room, Retrofit, OkHttp, kotlinx.serialization, WorkManager, DataStore, Security Crypto, Play Services Location, Mapbox Maps + Navigation, full testing stack).
- Wrote settings.gradle.kts — configured Mapbox Maven repo with auth.
- Wrote build.gradle.kts (root) + gradle.properties.
- Wrote app/build.gradle.kts — namespace com.waslni.driver, minSdk=24, targetSdk=34, Java 17, Compose enabled, BuildConfig for Mapbox token + API_BASE_URL (dev=10.0.2.2, prod=api.waslni.com).
- Wrote app/proguard-rules.pro — keep rules for Hilt, Room, Retrofit, kotlinx.serialization, Mapbox, coroutines.
- Wrote AndroidManifest.xml — permissions (INTERNET, LOCATION, POST_NOTIFICATIONS, FOREGROUND_SERVICE_LOCATION, VIBRATE), WaselApp as application name, MainActivity as launcher, WorkManager initializer disabled (Hilt-managed).
- Wrote WaselApp.kt — @HiltAndroidApp, Configuration.Provider for HiltWorkerFactory.
- Wrote MainActivity.kt — @AndroidEntryPoint, installSplashScreen, enableEdgeToEdge, hosts WaselNavHost inside WaselniTheme.
- Wrote core/ui/theme/Color.kt — brand palette (teal primary, orange secondary, yellow accent), Light + Dark semantic colors, map marker colors.
- Wrote core/ui/theme/Type.kt — Material 3 typography with larger body text for in-motion readability.
- Wrote core/ui/theme/Shape.kt — rounded shapes for friendly feel.
- Wrote core/ui/theme/Theme.kt — WaselniTheme() wrapper with light/dark color schemes, edge-to-edge window setup.
- Wrote presentation/navigation/Routes.kt — all route constants + BottomNavDestination enum with icons.
- Wrote presentation/navigation/WaselNavHost.kt — NavHost with splash/login/home/customers/history/settings/add-customer, Scaffold with bottom bar that hides on detail screens.
- Wrote presentation/auth/splash/SplashScreen.kt — 800ms delay then navigates to login (TODO Phase 11: real session check).
- Wrote presentation/auth/login/LoginScreen.kt — username + password fields with visibility toggle, login button, loading state.
- Wrote presentation/home/HomeScreen.kt — search bar + map placeholder + Add Customer FAB.
- Wrote presentation/customers/CustomerListScreen.kt — search bar + empty state + Add FAB.
- Wrote presentation/customers/AddCustomerScreen.kt — TopAppBar with back, name/phone/location fields, save button.
- Wrote presentation/delivery/HistoryScreen.kt — empty state.
- Wrote presentation/settings/SettingsScreen.kt — placeholder.
- Wrote di/AppModule.kt — provides WorkManager (Phase 3+ will add Database/Network/Repository modules).
- Wrote res/values/strings.xml (English default) + res/values-ar/strings.xml (Arabic) — app name, nav labels, login, customer fields, common actions.
- Wrote res/values/themes.xml — Theme.Waselni + Theme.Waselni.Starting (SplashScreen API).
- Wrote res/values/colors.xml — brand + background colors for splash.
- Wrote res/values/ic_launcher_background.xml.
- Wrote res/drawable/ic_splash_logo.xml — vector location pin for splash.
- Wrote res/drawable/ic_launcher_foreground.xml — vector for adaptive icon.
- Wrote res/mipmap-anydpi-v26/ic_launcher.xml + ic_launcher_round.xml — adaptive icons.
- Wrote res/xml/backup_rules.xml + data_extraction_rules.xml — exclude encrypted prefs + Room DB from cloud backup.
- Wrote android/README.md + local.properties.example + .gitignore.

Stage Summary:
- Phase 2 (Android Foundation) complete.
- 16 Kotlin files + 12 XML resource files + 5 Gradle/config files produced.
- App package: com.waslni.driver, applicationId: com.waslni.driver.debug (debug variant).
- Theme: Material 3, Dark/Light, Arabic RTL with system Sans font.
- Navigation: single-activity, NavHost with 7 routes, bottom nav on 4 main destinations.
- Next: Phase 3 (Local Data) — Room database with CustomerEntity, DeliveryEntity, SyncOperationEntity + DAOs + mappers + domain models + repository interfaces.

---
Task ID: phase-3
Agent: main
Task: Phase 3 — Local Data (Room): Build the complete local-first data layer with entities, DAOs, repositories, use cases, and Hilt wiring.

Work Log:
- Created directory tree: domain/{model,repository,usecase/customer}, data/local/{dao,entity,mapper}, data/repository, test/data/local/dao, test/domain/model.
- Wrote domain/model/Customer.kt — pure Kotlin data class with init-block validation (name 2-120, phone 7-30, lat -90..90, lng -180..180, accuracy >=0).
- Wrote domain/model/LatLng.kt — coordinate pair with Haversine distanceTo() for arrival detection.
- Wrote domain/model/LocationResult.kt — GPS fix result with isAcceptable(threshold) and DEFAULT_THRESHOLD=10m.
- Wrote domain/model/DeliveryStatus.kt — full state machine with isTerminal, isActive, allowedNextStates, canTransitionTo. Prevents illegal jumps like PENDING→DELIVERED.
- Wrote domain/model/Delivery.kt — delivery entity with isStateConsistent() that validates timestamp/status invariants.
- Wrote domain/model/SyncEnums.kt — SyncState (SYNCED/PENDING/FAILED) and SyncOperation (CREATE_CUSTOMER, UPDATE_CUSTOMER, DELETE_CUSTOMER, CREATE_DELIVERY, UPDATE_DELIVERY, COMPLETE_DELIVERY, CANCEL_DELIVERY).
- Wrote domain/model/SyncOperation.kt — sync queue item with idempotencyKey for COMPLETE_DELIVERY.
- Wrote data/local/entity/{CustomerEntity,DeliveryEntity,SyncOperationEntity}.kt — Room entities with indexes (phone, name, syncState, customerId, status, createdAt). FK RESTRICT on deliveries→customers.
- Wrote data/local/dao/CustomerDao.kt — observeAll, search (name OR phone, NOCASE), observeById, observeCount, observePendingSyncCount, getById, getByPhone, insert, update, delete, updateSyncState, bulkUpdateSyncState.
- Wrote data/local/dao/DeliveryDao.kt — observeAll, observeByDateRange, observeByStatus, observeByCustomer, observeById, observeCountByStatusSince, getActiveForCustomer, markStarted, markArrived, markCompleted, markCancelled.
- Wrote data/local/dao/SyncOperationDao.kt — getPending (oldest first), getPendingBatch, observePendingCount, incrementRetry (auto-FAILED at maxRetries via SQL CASE), cleanOldSynced, deletePendingForEntity.
- Wrote data/local/WaselDatabase.kt — @Database with 3 entities, version=1, exportSchema=true.
- Wrote data/local/WaselConverters.kt — placeholder for future type converters.
- Wrote data/local/mapper/{CustomerMapper,DeliveryMapper,SyncOperationMapper}.kt — bidirectional toDomain()/toEntity() with syncState handling.
- Wrote domain/repository/{CustomerRepository,DeliveryRepository,SyncRepository}.kt — interfaces with Flow for reactive reads and suspend for writes.
- Wrote data/repository/CustomerRepositoryImpl.kt — Offline-first: writes go to Room + enqueue SyncOperation. Duplicate phone detection. Active delivery check on delete.
- Wrote data/repository/DeliveryRepositoryImpl.kt — State machine enforcement via canTransitionTo. Auto-generates idempotencyKey for COMPLETE_DELIVERY.
- Wrote data/repository/SyncRepositoryImpl.kt — wraps SyncOperationDao with domain model mapping.
- Wrote 8 customer use cases: AddCustomerUseCase, UpdateCustomerUseCase, UpdateCustomerLocationUseCase, DeleteCustomerUseCase, ObserveCustomersUseCase, SearchCustomersUseCase, GetCustomerUseCase, CheckDuplicatePhoneUseCase.
- Wrote di/DatabaseModule.kt — provides WaselDatabase (singleton) + 3 DAOs.
- Wrote di/RepositoryModule.kt — @Binds for 3 repository interfaces.
- Wrote di/UseCaseModule.kt — @Provides for 8 use cases.
- Wrote di/SerializationModule.kt — provides kotlinx.serialization Json instance (ignoreUnknownKeys, encodeDefaults).
- Wrote 4 test files (39 test methods total):
  * CustomerDaoTest — 12 tests covering insert, getById, observeAll (sort), search (name/phone/empty), update, delete, getByPhone, observeCount, updateSyncState, getBySyncState, observePendingSyncCount.
  * DeliveryDaoTest — 9 tests covering insert, getActiveForCustomer, markCompleted, markCancelled, observeByDateRange, observeByStatus, observeCountByStatusSince, FK constraint violation.
  * SyncOperationDaoTest — 8 tests covering getPending (ordering), observePendingCount, updateStatus, markFailed, incrementRetry (with auto-FAILED), cleanOldSynced (preserves PENDING), getLatestForEntity, getPendingBatch.
  * DomainModelsTest — 18 tests covering Customer validation, DeliveryStatus state machine, LatLng.distanceTo Haversine, LocationResult.isAcceptable threshold logic.

Stage Summary:
- Phase 3 (Local Data) complete.
- 32 new Kotlin files (28 main + 4 test) added on top of Phase 2's 16.
- Total project: 52 Kotlin main files + 4 test files.
- Layered architecture now functional:
  * domain/model — pure Kotlin (no Android deps), validation in init blocks
  * data/local — Room layer (entities, DAOs, mappers, database)
  * data/repository — Offline-first implementations with sync queue wiring
  * domain/usecase — 8 customer use cases
  * di — 5 Hilt modules (App, Database, Repository, UseCase, Serialization)
- Key design decisions implemented:
  * No UNIQUE(phone) at DB level (allows offline duplicates before sync resolves) — enforced at repository layer instead.
  * FK RESTRICT on deliveries→customers prevents silent loss of delivery history.
  * SyncOperation payload is pre-serialized JSON at insert time (durable across app restarts).
  * COMPLETE_DELIVERY auto-generates an idempotencyKey.
  * State machine is enforced both in domain (DeliveryStatus.canTransitionTo) and repository (DeliveryRepositoryImpl.transition).
- Next: Phase 4 (GPS & Location) — build LocationProvider interface + FusedLocationProvider implementation + permission handling + accuracy threshold logic + GetCurrentLocationUseCase.

---
Task ID: phase-4
Agent: main
Task: Phase 4 — GPS & Location: Build the location subsystem with LocationProvider abstraction, FusedLocationProvider implementation, permission handling, accuracy threshold logic, CaptureLocationScreen, and unit tests.

Work Log:
- Created core/location/ package.
- Wrote LocationException.kt — sealed class hierarchy: LocationPermissionException (with permanentlyDenied flag), GpsDisabledException (with isResolvable), LocationTimeoutException (with timeoutMillis), LocationUnavailableException (fallback).
- Wrote LocationProvider.kt — interface with getCurrentLocation() (single fix with timeout/threshold/maxAge), observeLocationUpdates() (Flow for navigation), hasLocationPermission(), isLocationEnabled(). Constants: DEFAULT_TIMEOUT_MILLIS=10s, DEFAULT_ACCURACY_THRESHOLD=10m, DEFAULT_MAX_AGE_MILLIS=30s.
- Wrote FusedLocationProvider.kt — production implementation using Play Services FusedLocationProviderClient. Strategy: try last known location first (cheap) → request fresh high-accuracy fix if stale/poor → use withTimeoutOrNull for deadline. Uses LocationRequest.Builder with PRIORITY_HIGH_ACCURACY, setWaitForAccurateLocation(true). Converts android.location.Location to domain LocationResult.
- Wrote LocationModule.kt — Hilt module providing FusedLocationProvider (singleton) + GpsSettingsHelper. GpsSettingsHelper has openLocationSettings() (ACTION_LOCATION_SOURCE_SETTINGS) and openAppDetailsSettings() (ACTION_APPLICATION_DETAILS_SETTINGS for permanently denied case).
- Wrote domain/usecase/location/GetCurrentLocationUseCase.kt — thin wrapper that delegates to LocationProvider with same parameters.
- Updated di/UseCaseModule.kt to provide GetCurrentLocationUseCase.
- Wrote core/ui/location/PermissionState.kt — enum (UNKNOWN, GRANTED, DENIED, PERMANENTLY_DENIED) + rememberPermissionState() composable using ActivityResultContracts.RequestMultiplePermissions. Heuristic: denialCount >= 2 → PERMANENTLY_DENIED (system won't show dialog anymore).
- Wrote core/ui/components/ConfirmationDialog.kt — generic AlertDialog wrapper with title/message/confirm/dismiss.
- Wrote core/ui/components/AccuracyIndicator.kt — visual indicator with 3 tiers (EXCELLENT ≤5m, GOOD ≤threshold, POOR >threshold). Pure computeAccuracyTier() function extracted for testability. Green check icon for acceptable, orange warning for poor.
- Wrote presentation/location/LocationPermissionGate.kt — composable that wraps a screen requiring location permission. Shows rationale dialog first, then system permission dialog, then falls back to "open Settings" dialog for permanently denied.
- Wrote presentation/location/CaptureLocationViewModel.kt — HiltViewModel with sealed CaptureLocationUiState (Idle, Loading, Success, PoorAccuracy, Error). captureLocation() runs the use case, checks accuracy against threshold, maps exceptions to LocationErrorType enum.
- Wrote presentation/location/CaptureLocationScreen.kt — full screen UI with map placeholder, location coordinates display, AccuracyIndicator, capture button, retry button, error messages. Uses StateFlow from ViewModel via collectAsStateWithLifecycle.
- Updated presentation/navigation/Routes.kt — added CAPTURE_LOCATION route.
- Updated presentation/navigation/WaselNavHost.kt — added composable for CaptureLocationScreen wrapped in LocationPermissionGate.
- Added 23 new string resources in values/strings.xml (English) and values-ar/strings.xml (Arabic) for: location rationale, permission denied, GPS disabled, timeout, capture flow, accuracy tiers (excellent/good/poor).
- Wrote 4 test files (35 test methods):
  * FakeLocationProvider.kt — test double with permissionGranted, locationEnabled, locationToReturn, exceptionToThrow, updatesToEmit knobs.
  * GetCurrentLocationUseCaseTest.kt — 9 tests: returns location, default params propagation, custom params propagation, exception pass-through (permission/gps/timeout/runtime), call count.
  * CaptureLocationViewModelTest.kt — 14 tests using Turbine: Idle initial state, Success on acceptable accuracy, PoorAccuracy on poor accuracy, threshold boundary, exception mapping (permission/gps/timeout/unknown), reset(), multiple sequential captures.
  * AccuracyIndicatorTest.kt — 9 tests: tier boundaries (0/5/5.1/10/10.1/70), threshold adjustment effects, EXCELLENT independence from threshold.

Stage Summary:
- Phase 4 (GPS & Location) complete.
- 12 new Kotlin main files + 4 new test files added on top of Phase 3.
- Total project: 63 Kotlin main files + 8 test files = 71 Kotlin files.
- Location subsystem architecture:
  * core/location/ — LocationProvider interface + FusedLocationProvider impl + LocationException hierarchy + GpsSettingsHelper + Hilt LocationModule
  * core/ui/location/ — PermissionState enum + rememberPermissionState composable
  * core/ui/components/ — AccuracyIndicator (3-tier) + ConfirmationDialog
  * presentation/location/ — LocationPermissionGate, CaptureLocationScreen, CaptureLocationViewModel
  * domain/usecase/location/ — GetCurrentLocationUseCase
- Key design decisions:
  * Last-known-location-first strategy: try cheap instant fix before requesting fresh one (saves battery + time).
  * setWaitForAccurateLocation(true) on LocationRequest.Builder — Play Services will internally wait for a high-accuracy fix instead of returning the first noisy one.
  * Accuracy threshold is parameter-driven (default 10m, configurable in Settings later) — use case and UI both honor the same value.
  * Exception types map cleanly to UI states: each LocationException subclass has a corresponding LocationErrorType enum value.
  * Permission state has 4 distinct states (UNKNOWN, GRANTED, DENIED, PERMANENTLY_DENIED) — UI shows the right dialog for each.
  * FakeLocationProvider exposes knobs for every behavior the tests need to drive (no Robolectric required for use case/VM tests).
- Next: Phase 5 (Maps) — Mapbox integration: MapProvider interface, MapboxMapProvider impl, HomeScreen with full-screen map, driver marker, customer markers from Room, marker interaction.

---
Task ID: phase-5
Agent: main
Task: Phase 5 — Maps (Mapbox): Integrate Mapbox, build MapProvider abstraction, render driver + customer markers on the Home screen with marker interaction (bottom sheet).

Work Log:
- Created core/maps/ package with model/, mapbox/ subpackages.
- Wrote core/maps/model/MapModels.kt:
  * MarkerType enum (DRIVER, CUSTOMER, ACTIVE, DELIVERED, CANCELLED)
  * MapMarker data class (id, position, type, data)
  * CameraTarget sealed interface (Center / Fit)
  * MapTapResult sealed interface (OnMarker / OnPoint)
- Wrote core/maps/MapProvider.kt — interface with attach(), detach(), setMarkers(), moveCamera(), observeTaps() Flow, isReady() Flow. Lifecycle contract: attach() once per host, detach() releases resources.
- Wrote core/maps/mapbox/MapboxMapProvider.kt — production implementation:
  * Uses MapView (owned by Composable via AndroidView), bind(mapView) method
  * Loads Style.MAPBOX_STREETS, sets _isReady=true on style load
  * Uses PointAnnotationManager for markers (single manager, diffable updates)
  * OnMapClickListener with hitTest() — finds closest marker within 50m radius
  * Marker colors via colorHex() extension matching Color.kt constants
  * Camera flyTo for Center, cameraForCoordinates + flyTo for Fit
- Wrote core/maps/MapsModule.kt — Hilt module binding MapboxMapProvider as singleton MapProvider.
- Wrote domain/usecase/delivery/ObserveActiveDeliveryCustomerIdsUseCase.kt — streams Set<String> of customer IDs with active deliveries (ON_THE_WAY). Used to color those markers as ACTIVE.
- Updated di/UseCaseModule.kt to provide the new use case.
- Wrote presentation/home/HomeViewModel.kt:
  * HiltViewModel injecting ObserveCustomersUseCase, ObserveActiveDeliveryCustomerIdsUseCase, LocationProvider
  * Combines 4 flows: customers, activeCustomerIds, driverLocation, selectedCustomerId → single HomeUiState
  * startObservingDriverLocation() — low-frequency 15s updates when permission granted
  * onMarkerClicked(id) / onDismissSelection() for bottom sheet state
  * HomeUiState.markers() pure function deriving MapMarker list (DRIVER first, then customers colored by active state)
- Rewrote presentation/home/HomeScreen.kt:
  * Acquires MapProvider via Hilt EntryPoint (composables can't @Inject)
  * MapProviderEntryPoint interface for the entry point
  * AndroidView factory creates MapView, calls MapboxMapProvider.bind()
  * LaunchedEffect collects isReady() and observeTaps()
  * LaunchedEffect on state changes → setMarkers() + moveCamera() to driver
  * LocationPermissionGate wraps the map (still shows map if denied, just no driver marker)
  * Empty state hint when customers list is empty
  * "My location" FAB (bottom-end) + "Add customer" FAB (bottom-start)
  * ModalBottomSheet for selected customer: name, phone, coordinates, Start/Call/Details buttons
  * Call button uses Intent.ACTION_DIAL with tel: URI
  * DisposableEffect.onDispose calls mapProvider.detach()
- Added 7 new string resources in values/ and values-ar/ for: map_driver_location, map_customer_marker, map_active_marker, map_no_customers_hint, customer_call, customer_start_delivery, customer_view_details.
- Wrote 3 test files (32 test methods):
  * FakeMapProvider.kt — test double capturing lastMarkers, lastCameraTarget, call counts; supports emitTap() and setReady().
  * MapProviderContractTest.kt — 11 tests: attach/detach counts, setMarkers capture and replace, moveCamera with Center/Fit targets, isReady flow, observeTaps emits OnMarker/OnPoint, initial state.
  * HomeUiStateMarkersTest.kt — 11 tests: empty state, DRIVER marker from driverLocation, CUSTOMER vs ACTIVE based on activeCustomerIds, driver marker first, marker positions, multi-customer order, ghost-active-id ignored, data payload correctness.

Stage Summary:
- Phase 5 (Maps) complete.
- 6 new Kotlin main files + 3 new test files added on top of Phase 4.
- Total project: 69 Kotlin main files + 11 test files = 80 Kotlin files.
- Map architecture:
  * core/maps/model/ — MarkerType, MapMarker, CameraTarget, MapTapResult
  * core/maps/MapProvider — interface (SDK-agnostic)
  * core/maps/mapbox/MapboxMapProvider — production impl using Mapbox SDK
  * core/maps/MapsModule — Hilt binding
  * presentation/home/HomeViewModel — combines customer + delivery + location flows → markers
  * presentation/home/HomeScreen — full-screen map + FABs + bottom sheet
- Key design decisions:
  * MapView is owned by the Composable (via AndroidView), not by the provider. Provider binds to it via bind(mapView). This keeps Compose lifecycle in control.
  * Marker IDs are stable ("driver", "customer-{id}"). Mapbox SDK diffs by these.
  * Tap hit-testing uses 50m Haversine radius — forgiving for fingers on small markers.
  * Driver marker rendered first so customer markers paint on top.
  * activeCustomerIds drives marker color (CUSTOMER → ACTIVE) without touching domain models.
  * Composables acquire MapProvider via Hilt EntryPoint (composables can't @Inject).
  * Low-frequency driver updates (15s) on home screen for battery. Higher frequency (3s) reserved for navigation in Phase 15.
- Next: Phase 6 (Customer Management) — full CRUD: AddCustomerViewModel wired to AddCustomerUseCase, real customer list with search, edit/delete customer screens, customer details screen, duplicate phone detection, location capture integrated into add flow.

---
Task ID: phase-6
Agent: main
Task: Phase 6 — Customer Management: Wire ViewModels to real UseCases for full CRUD (list with search, add with GPS capture + duplicate detection, details with delete confirmation, edit with location update).

Work Log:
- Wrote presentation/customers/CustomerListViewModel.kt:
  * HiltViewModel injecting ObserveCustomersUseCase + SearchCustomersUseCase
  * Uses flatMapLatest on _query flow: blank → observeAll, non-blank → search
  * CustomerListUiState with computed isEmpty / isSearching / isNoResults
- Rewrote presentation/customers/CustomerListScreen.kt:
  * Real OutlinedTextField bound to viewModel::onQueryChange
  * Clear button (X icon) when query non-empty
  * LazyColumn with CustomerRow (avatar circle with first letter + name + phone + coordinates)
  * Empty state when no customers, no-results state when search returns empty
  * FAB to add customer
- Wrote presentation/customers/AddCustomerViewModel.kt:
  * HiltViewModel injecting AddCustomerUseCase + GetCurrentLocationUseCase + CheckDuplicatePhoneUseCase
  * State machine: Idle → CapturingLocation → Idle (with location) → Saving → Saved
  * canSave computed property: name 2-120, phone 7-30, location != null, not saving/capturing
  * Duplicate phone check before save (returns existing customer's name in error)
  * validateName() + validatePhone() + Throwable.toUserMessage() extracted as internal helpers for testability
- Rewrote presentation/customers/AddCustomerScreen.kt:
  * OutlinedTextField for name + phone with supportingText errors
  * LocationPermissionGate wrapping the location section
  * Location section: capture button / captured display + AccuracyIndicator / re-capture button
  * SnackbarHost for locationError + errorMessage
  * LaunchedEffect on isSaved → onSaved() callback
- Wrote presentation/customers/CustomerDetailsViewModel.kt:
  * HiltViewModel injecting GetCustomerUseCase + DeleteCustomerUseCase
  * _customerId flow + flatMapLatest on customer flow
  * AuxState (isDeleting, isDeleted, deleteError, showDeleteConfirm) combined with customer flow
  * delete() runs DeleteCustomerUseCase, sets isDeleted on success or deleteError on failure
- Wrote presentation/customers/CustomerDetailsScreen.kt:
  * TopAppBar with back + edit action
  * Customer info: name (headline), phone (with dial intent), coordinates, GPS accuracy (AccuracyIndicator), last updated
  * Primary: Start Delivery button (Phase 13 wires)
  * Secondary: Call button (Intent.ACTION_DIAL)
  * Tertiary row: Edit + Delete buttons
  * ConfirmationDialog for delete with customer name in message
  * Loading state (CircularProgressIndicator) while customer loads
  * SnackbarHost for deleteError
- Wrote presentation/customers/EditCustomerViewModel.kt:
  * HiltViewModel injecting GetCustomerUseCase + UpdateCustomerUseCase + UpdateCustomerLocationUseCase + GetCurrentLocationUseCase
  * FormState tracks name/phone/location/error flags separately from originalCustomer
  * First load pre-fills form from customer flow; subsequent edits preserve user input
  * hasUnsavedChanges computed property
  * save() calls UpdateCustomerUseCase (if name/phone changed) then UpdateCustomerLocationUseCase (if location re-captured)
- Wrote presentation/customers/EditCustomerScreen.kt:
  * Same form layout as Add (name + phone + location)
  * Location section shows existing customer location with "Update location" button
  * After re-capture: shows new location with AccuracyIndicator + "Re-capture" button
  * Loading state while customer loads
- Updated presentation/navigation/WaselNavHost.kt:
  * Added composable for Routes.CUSTOMER_DETAILS with customerId NavType.StringType argument
  * Added composable for Routes.EDIT_CUSTOMER with customerId argument
  * Wired onEdit and onDeleted callbacks
- Added 13 new string resources in values/ and values-ar/ for: customer details title, GPS accuracy, last updated, edit/delete actions, delete confirm title + message, customer loading, customer not found, edit customer title, update location.
- Wrote 3 test files (40 test methods):
  * ValidationHelpersTest.kt — 21 tests covering validateName (4 cases), validatePhone (5 cases), Throwable.toUserMessage (6 cases for each LocationException subclass), AddCustomerUiState.canSave (6 cases)
  * CustomerListUiStateTest.kt — 9 tests covering isEmpty, isSearching, isNoResults, total
  * AddCustomerViewModelTest.kt — 10 tests using FakeCustomerRepository + FakeLocationProvider:
    - Initial state
    - onNameChange/onPhoneChange
    - captureLocation success + timeout
    - canSave becomes true when valid
    - save calls repository and sets isSaved
    - save with duplicate phone sets phoneError
    - save with short name/phone sets validation errors
    - resetSaved clears flag
  * FakeCustomerRepository — minimal fake tracking addedCustomers + existingByPhone for duplicate testing.

Stage Summary:
- Phase 6 (Customer Management) complete.
- 8 new Kotlin main files + 3 new test files added on top of Phase 5.
- Total project: 75 Kotlin main files + 14 test files = 89 Kotlin files.
- Customer CRUD fully wired:
  * List with reactive search (flatMapLatest on query flow)
  * Add with GPS capture + duplicate phone detection + form validation
  * Details with delete confirmation + Call action
  * Edit with name/phone update + location re-capture
- Navigation graph now has 9 routes: splash, login, home, customers, history, settings, add_customer, customer_details, edit_customer, capture_location.
- Validation helpers (validateName, validatePhone, toUserMessage) extracted as internal functions for direct unit testing without ViewModel instantiation.
- Key UX decisions:
  * Avatar circle uses first letter of name — gives visual variety in the list without icon assets
  * Delete uses a confirmation dialog (not a swipe action) because customer deletion is destructive and blocked by active deliveries
  * Edit screen pre-fills form from customer flow and re-emits when Room updates (form stays in sync if customer changes from another flow)
  * Location re-capture is a separate button in Edit (not automatic) because we want explicit user intent for GPS updates
- Next: Phase 7 (Backend Foundation) — FastAPI project setup, PostgreSQL schema migrations, docker-compose, Alembic, seed script, /health endpoint.
