# Cola Tracker UI Atlas

## Navigation Graph
- **Host**: `MainActivity`
- **Start Destination**: `children_list`

### Routes
1.  **Children List** (`children_list`)
    - **Screen**: `ChildrenListScreen`
    - **ViewModel**: `ChildrenListViewModel`
    - **Features**:
        - List of `ChildCard` components.
        - Swipe-to-refresh to reload data.
        - Click on card navigates to Detail.

2.  **Child Detail** (`child_detail/{childJson}`)
    - **Screen**: `ChildDetailScreen`
    - **ViewModel**: `ChildDetailViewModel`
    - **Arguments**: `childJson` (Serialized `Child` object)
    - **Features**:
        - **Header**: Avatar and dynamic "Remaining" limit display.
        - **Quick Actions**: Buttons for 250ml, 330ml, and Custom amount.
        - **History List**: Scrollable list of recent drinks.
        - **Actions**: Delete history item (swipe or click).

## Key Components

### ChildCard
- **Path**: `ui/components/ChildCard.kt`
- **Usage**: Used in `ChildrenListScreen` to display a summary of a child.
- **Visuals**:
    - Circular Avatar.
    - Name and Limit text.
    - Linear Progress Indicator (color changes based on consumption).

### Theme
- **Path**: `ui/theme/`
- **System**: Material Design 3
- **Support**: Light and Dark mode auto-switch.
