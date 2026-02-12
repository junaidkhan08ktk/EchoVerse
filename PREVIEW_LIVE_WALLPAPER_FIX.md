# Preview vs Live Wallpaper Appearance Fix

## Problem
Wallpapers looked great in the **preview screen** but appeared different (dimmer, less animated) when applied as the **live wallpaper**.

## Root Cause
The issue was caused by two main differences:

1. **Preview always simulates audio**: The preview screen continuously generates fake audio data for demonstration, keeping wallpapers in ACTIVE/PEAK modes
2. **Live wallpaper respects silence**: The actual wallpaper service uses real audio input, which defaults to IDLE mode when there's no sound

This meant:
- **Preview**: Always animated, bright, full-scale visuals
- **Live Wallpaper**: Dim, minimal movement in IDLE mode without audio

## Solution Implemented

### 1. Enhanced IDLE Mode Parameters

**File**: `WorldBehaviorEngine.kt`

**Before**:
```kotlin
WorldMode.IDLE -> VisualParameters(
    particleSpeed = 0.5f * speedMult,
    masterScale = 0.2f,           // Very small
    colorIntensity = 0.2f,        // Very dim
    distortion = 0.0f,            // No variation
    spawnRate = 0.8f
)
```

**After** (Category-specific):
```kotlin
WorldMode.IDLE -> {
    when (category) {
        WorldCategory.FLUID -> VisualParameters(
            particleSpeed = 0.8f * speedMult,   // Keep flowing
            masterScale = 1.0f,                  // Full scale!
            colorIntensity = 0.6f,               // Visible colors
            distortion = 0.2f,                   // Subtle variation
            spawnRate = 1.0f                     // Active particles
        )
        WorldCategory.CALM -> VisualParameters(
            particleSpeed = 0.5f * speedMult,
            masterScale = 0.9f,                  // Nearly full scale
            colorIntensity = 0.4f,
            distortion = 0.05f,
            spawnRate = 0.8f
        )
        // ... other categories
    }
}
```

**Impact**: Wallpapers now look great even without audio input!

### 2. Enhanced Background Visibility

**File**: `EchoWallpaperService.kt` - `drawBackground()`

**Before**:
```kotlin
fillPaint.alpha = (20 + currentParams.colorIntensity * 40 + pulse * 50)
```

**After**:
```kotlin
fillPaint.alpha = (40 + currentParams.colorIntensity * 60 + pulse * 50)
```

**Changes**:
- Base alpha: **20 → 40** (doubled)
- Color intensity multiplier: **40 → 60** (50% increase)
- Result: Brighter, more vibrant background gradient

### 3. Enhanced Fluid Wallpaper Alpha Values

**File**: `EchoWallpaperService.kt` - Fluid rendering functions

Updated baseline alpha for better visibility without audio:

| Wallpaper | Element | Old Base | New Base | Improvement |
|-----------|---------|----------|----------|-------------|
| **Liquid Dream** | Metaballs | 150 | **180** | +20% |
| **Vapor Flow** | Particles | Variable | **+100 min** | Always visible |
| **Plasma Wave** | Primary wave | 127 | **150** | +18% |
| **Plasma Wave** | Secondary wave | 76 | **100** | +31% |
| **Oil Canvas** | Paint swirls | 100 | **120** | +20% |

**Result**: All fluid wallpapers are now clearly visible even in silence.

## Technical Details

### IDLE Mode Philosophy Change

**Old approach**: 
- "No sound = minimal visuals" 
- Made sense for audio-reactive principle
- But resulted in boring wallpapers when quiet

**New approach**:
- "Always beautiful, audio enhances"
- Wallpapers have intrinsic beauty
- Audio makes them MORE dynamic, not ONLY dynamic

### Category-Specific Tuning

Different wallpaper categories now have different IDLE behaviors:

- **FLUID**: Highest idle activity (must keep flowing)
- **NATURE**: Medium idle activity (nature is always alive)
- **CALM**: Lower idle activity (peaceful by design)
- **ENERGETIC**: Medium idle activity
- **ABSTRACT**: Medium idle activity

This ensures each category feels appropriate even without audio.

## Comparison

### Before Fix

**Without Audio (IDLE mode)**:
- ❌ Very dim background (alpha ~20-60)
- ❌ Tiny particles (scale 0.2)
- ❌ Minimal movement
- ❌ Low color intensity
- ❌ Fluid wallpapers barely visible

**With Audio (ACTIVE mode)**:
- ✅ Bright, animated
- ✅ Dynamic response to music

### After Fix

**Without Audio (IDLE mode)**:
- ✅ Visible background (alpha ~40-100)
- ✅ Full-scale visuals (scale 0.9-1.0)
- ✅ Continuous smooth animation
- ✅ Good color saturation
- ✅ Fluid wallpapers clearly visible and flowing

**With Audio (ACTIVE/PEAK mode)**:
- ✅ Even brighter and more dynamic
- ✅ Reactive to music beats

## Testing Checklist

To verify the fix works:

1. **Apply any wallpaper** as live wallpaper
2. **Ensure no music is playing** and stay quiet
3. **Observe the wallpaper** - it should:
   - ✅ Have visible, vibrant colors
   - ✅ Show smooth, continuous animation
   - ✅ Be clearly visible (not dim or faded)
   - ✅ Look similar to the preview
4. **Play music** - wallpaper should:
   - ✅ Become even more dynamic
   - ✅ React to beats and frequencies
   - ✅ Increase in intensity

5. **Test FLUID wallpapers specifically**:
   - Liquid Dream: Blobs should flow smoothly
   - Vapor Flow: Particles should drift organically
   - Plasma Wave: Waves should flow continuously
   - Mercury Dance: Droplets should move fluidly
   - Oil Canvas: Paint should swirl constantly

## Files Modified

1. ✅ `WorldBehaviorEngine.kt` - IDLE mode parameters
2. ✅ `EchoWallpaperService.kt` - Background alpha
3. ✅ `EchoWallpaperService.kt` - Fluid rendering alpha values

## Result

The wallpapers now look **beautiful and animated both in preview AND as live wallpaper**, with or without audio input. Audio enhances the experience but is no longer required for visual appeal.

**Key principle**: "Always stunning, audio amplifies" instead of "Only alive with audio"
