package com.xprox.sentinel.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// Unified Deep Space Stealth & Cosmic Cyberpunk Design System Palette
// Aligned 1:1 with x-pc (Tauri/React) and panel (Spectre Web Panel)
// ============================================================================

// Main Void & Cosmic Backdrops
val VoidBg = Color(0xFF06060A)             // Primary Void Dark (#06060a)
val DarkBg = VoidBg                        // Alias for backward compatibility
val CosmicNightBg = Color(0xFF08061A)      // Cosmic Deep Base (#08061a)
val CosmicPurpleAura = Color(0xFF150938)   // Radial Top Violet Aura (#150938)

// Surface & Card Core Colors
val DarkCard = Color(0xFF0D0D15)           // Primary Card Core (#0d0d15)
val DarkCardElevated = Color(0xFF131320)   // Elevated Surface Core (#131320)
val CosmicGlassBg = Color(0x73120C2A)      // Glassmorphism Overlay (rgba(18, 12, 42, 0.45))

// Doppelrand (Double Bezel) Architecture Tokens
val DoppelrandShellBg = Color(0x08FFFFFF)   // Outer Shell Background (rgba(255, 255, 255, 0.03))
val DoppelrandShellBorder = Color(0x12FFFFFF) // Outer Shell Border (rgba(255, 255, 255, 0.07))
val DoppelrandShellBorderHover = Color(0x24FFFFFF) // Outer Shell Hover (rgba(255, 255, 255, 0.14))
val DoppelrandCoreBg = Color(0xFF0D0D15)    // Inner Core Background (#0d0d15)

// Accent & Glow Colors
val ElectricViolet = Color(0xFF8B5CF6)     // Electric Neon Violet (#8b5cf6) - Primary Accent
val CyberPurple = ElectricViolet           // Alias for backward compatibility
val ElectricVioletGlow = Color(0x358B5CF6) // Soft Neon Violet Glow

val CyberCyan = Color(0xFF06B6D4)          // Neon Cyan (#06b6d4)
val CyberTeal = CyberCyan                  // Alias for backward compatibility
val CyberBlue = Color(0xFF6366F1)          // Slate Indigo (#6366f1)

val SecureGreen = Color(0xFF10B981)        // Emerald Active/Protected (#10b981)
val WarningRose = Color(0xFFF43F5E)        // Ember Rose Disconnected/Error (#f43f5e)
val WarningRed = WarningRose               // Alias for backward compatibility
val WarningAmber = Color(0xFFF59E0B)       // Warm Amber Connecting/Warning (#f59e0b)
val WarningYellow = WarningAmber           // Alias for backward compatibility

// Typography & Text Colors
val TextWhite = Color(0xFFF8FAFC)          // Slate Primary Text (#f8fafc)
val TextPrimary = TextWhite
val TextSecondary = Color(0xFFCBD5E1)      // Slate Secondary Text (#cbd5e1)
val TextGray = Color(0xFF64748B)           // Muted Slate Text (#64748b)
val TextMuted = TextGray
val TextDim = Color(0xFF475569)            // Subdued Text (#475569)

// Borders & Dividers
val CardBorder = Color(0x14FFFFFF)         // Standard Surface Border (rgba(255, 255, 255, 0.08))
val BorderHighlight = Color(0x738B5CF6)     // Violet Border Highlight (rgba(139, 92, 246, 0.45))
