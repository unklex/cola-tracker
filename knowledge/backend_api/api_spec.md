# Cola Tracker API Documentation

## Base URL
Development: `http://<HOST_IP>:8000`

## Authentication
- **Type**: Bearer Token
- **Header**: `Authorization: Bearer <TOKEN>`
- **Token Location**: Stored in `data.json` on the server and `AppConfig.kt` in the app.

## Endpoints

### Children
- **GET /children**
    - Returns list of children with current stats (remaining limit, consumed).
    - response: `List<ChildResponse>`

### Drinks
- **POST /children/{child_id}/drink**
    - Add a drink entry.
    - Body: `{"amount_ml": int}`
    - Updates `consumed_this_month` and `remaining`.
- **DELETE /drinks/{drink_id}**
    - Remove a drink entry.
    - Restores the limit to the child's balance.

### History
- **GET /children/{child_id}/history**
    - Get consumption history for a child.
    - Sorted by timestamp (newest first).

### Photos
- **POST /children/{child_id}/photo**
    - Upload a child's photo.
    - Format: Multipart/form-data.
- **GET /photos/{filename}**
    - Retrieve a photo.

## Data Models
- **ChildResponse**: `id`, `name`, `photo_url`, `monthly_limit`, `consumed_this_month`, `remaining`.
- **DrinkRequest**: `amount_ml`.
- **DrinkHistoryItem**: `id`, `child_id`, `amount_ml`, `timestamp`.
