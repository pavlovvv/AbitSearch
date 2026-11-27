package ua.kpi.abitsearch.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.kpi.abitsearch.domain.model.YearStat
import ua.kpi.abitsearch.ui.theme.Abit
import ua.kpi.abitsearch.ui.theme.budgetColor
import ua.kpi.abitsearch.ui.theme.contractColor

/** Градієнтний фон з двома розмитими «плямами» кольору, як на макетах. */
@Composable
fun GradientBackground(content: @Composable BoxScope.() -> Unit) {
    val c = Abit.colors
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(c.bgTop, c.bgBottom, c.bgTop)))
    ) {
        Box(
            Modifier
                .size(360.dp)
                .align(Alignment.TopEnd)
                .padding(0.dp)
                .background(Brush.radialGradient(listOf(c.primaryEnd.copy(alpha = if (c.isDark) 0.45f else 0.25f), Color.Transparent)))
        )
        Box(
            Modifier
                .size(320.dp)
                .align(Alignment.CenterStart)
                .background(Brush.radialGradient(listOf(Color(0xFF2E6BFF).copy(alpha = if (c.isDark) 0.25f else 0.15f), Color.Transparent)))
        )
        content()
    }
}

/** Скляна поверхня: напівпрозора заливка + тонка рамка + заокруглення. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    radius: Dp = 20.dp,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    padding: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Abit.colors
    val shape = RoundedCornerShape(radius)
    Column(
        modifier
            .clip(shape)
            .background(if (highlighted) c.primary.copy(alpha = 0.16f) else c.glass)
            .border(1.dp, if (highlighted) c.primary.copy(alpha = 0.55f) else c.outline, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

@Composable
fun LargeTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.displaySmall,
        color = Abit.colors.text,
        modifier = modifier.padding(top = 8.dp, bottom = 12.dp, start = 2.dp),
    )
}

@Composable
fun CircleIconButton(icon: ImageVector, contentDescription: String, tint: Color = Abit.colors.text, onClick: () -> Unit) {
    val c = Abit.colors
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(c.glass)
            .border(1.dp, c.outline, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(20.dp)) }
}

/** Верхня панель екрана деталей: «Назад», заголовок, дія праворуч. */
@Composable
fun DetailTopBar(title: String, onBack: () -> Unit, action: (@Composable () -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Назад", onClick = onBack)
        Text(
            title,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            color = Abit.colors.text,
        )
        if (action != null) action() else Spacer(Modifier.width(40.dp))
    }
}

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val c = Abit.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(shape)
            .background(c.glass)
            .border(1.dp, if (value.isNotEmpty()) c.primary else c.outline, shape)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, null, tint = c.muted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, color = c.muted, style = MaterialTheme.typography.bodyLarge)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = c.text, fontSize = 15.sp),
                cursorBrush = SolidColor(c.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                Icons.Filled.Close, "Очистити", tint = c.muted,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onValueChange("") },
            )
        }
    }
}

@Composable
fun AbitChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val c = Abit.colors
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .clip(shape)
            .then(
                if (selected) Modifier.background(Brush.linearGradient(listOf(c.primary, c.primaryEnd)))
                else Modifier
                    .background(c.glass)
                    .border(1.dp, c.outline, shape)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text,
            color = if (selected) Color.White else c.text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun ChipRow(content: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
fun <T> Segmented(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val c = Abit.colors
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.glass)
            .border(1.dp, c.outline, shape)
            .padding(3.dp),
    ) {
        options.forEach { option ->
            val on = option == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (on) c.glassStrong else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label(option), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (on) c.text else c.muted)
            }
        }
    }
}

/** Невелика кольорова мітка: «Акредитовано», «Набір 2025» тощо. */
@Composable
fun Badge(text: String, color: Color) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(5.dp))
        Text(text, color = color, style = MaterialTheme.typography.labelSmall)
    }
}

/**
 * Стовпчикова діаграма зарахування за роками: нижня частина стовпчика - бюджет, верхня - контракт.
 * Малюється на Canvas без сторонніх бібліотек графіків.
 */
@Composable
fun EnrollmentChart(stats: List<YearStat>, modifier: Modifier = Modifier) {
    val c = Abit.colors
    val budget = budgetColor(c.isDark)
    val contract = contractColor(c.isDark)
    val max = (stats.maxOfOrNull { it.total } ?: 0).coerceAtLeast(1)
    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(140.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            stats.forEach { s ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(s.total.toString(), color = c.muted, fontSize = 9.sp, maxLines = 1)
                    Spacer(Modifier.height(2.dp))
                    Canvas(
                        Modifier
                            .fillMaxWidth()
                            .height(118.dp)
                    ) {
                        val h = size.height * s.total / max
                        val hb = size.height * s.budget / max
                        val r = CornerRadius(6.dp.toPx())
                        if (h > 0f) drawRoundRect(contract, topLeft = Offset(0f, size.height - h), size = Size(size.width, h), cornerRadius = r)
                        if (hb > 0f) drawRoundRect(budget, topLeft = Offset(0f, size.height - hb), size = Size(size.width, hb), cornerRadius = r)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            stats.forEach { s ->
                Text("'" + (s.year % 100).toString().padStart(2, '0'), color = c.faint, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            LegendDot(budget); Text(" бюджет", color = c.muted, fontSize = 12.sp)
            Spacer(Modifier.width(14.dp))
            LegendDot(contract); Text(" контракт", color = c.muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun LegendDot(color: Color) {
    Box(
        Modifier
            .size(9.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun CodeTag(code: String) {
    val c = Abit.colors
    Text(
        code,
        color = c.primary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(c.glassStrong)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
fun Avatar(text: String, size: Dp = 44.dp, circle: Boolean = false) {
    val shape = if (circle) CircleShape else RoundedCornerShape(14.dp)
    Box(
        Modifier
            .size(size)
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF9B8CFF), Color(0xFF5B8CFF)))),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.32f).sp, maxLines = 1)
    }
}

/** Українська форма множини: plural(2, "заява", "заяви", "заяв") → «2 заяви». */
fun plural(n: Int, one: String, few: String, many: String): String {
    val form = when {
        n % 100 in 11..14 -> many
        n % 10 == 1 -> one
        n % 10 in 2..4 -> few
        else -> many
    }
    return "$n $form"
}

/** Ініціали або абревіатура назви: «КПІ ім. Ігоря Сікорського» → «КПІ». */
fun abbreviation(name: String): String {
    val letters = name.filter { it.isLetter() || it == ' ' || it == '-' }.trim()
    val firstToken = letters.split(" ").firstOrNull().orEmpty()
    // Коротка назва на кшталт «КПІ» або «ЛНУ» - показуємо як є.
    if (firstToken.length in 2..5 && firstToken.count { it.isUpperCase() } >= 2) return firstToken.uppercase().take(4)
    return letters.split(" ", "-")
        .filter { it.length > 2 && it.first().isLetter() }
        .take(3)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifEmpty { letters.take(2).uppercase() }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = Abit.colors.muted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 8.dp),
    )
}

@Composable
fun SubtitleText(text: String, modifier: Modifier = Modifier, maxLines: Int = 2) {
    Text(
        text, color = Abit.colors.muted, style = MaterialTheme.typography.labelMedium,
        maxLines = maxLines, overflow = TextOverflow.Ellipsis, modifier = modifier,
    )
}

@Composable
fun TitleText(text: String, modifier: Modifier = Modifier, maxLines: Int = 3) {
    Text(
        text, color = Abit.colors.text, style = MaterialTheme.typography.titleMedium,
        maxLines = maxLines, overflow = TextOverflow.Ellipsis, modifier = modifier,
    )
}

/** Повідомлення про стан на весь екран: порожньо або помилка. */
@Composable
fun MessageState(
    icon: ImageVector,
    title: String,
    text: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    iconTint: Color = Abit.colors.primary,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 30.dp, vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        GlassCard(radius = 24.dp, padding = 20.dp) { Icon(icon, null, tint = iconTint, modifier = Modifier.size(34.dp)) }
        Text(title, color = Abit.colors.text, fontSize = 19.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(text, color = Abit.colors.muted, fontSize = 13.sp, textAlign = TextAlign.Center)
        if (actionText != null && onAction != null) PrimaryButton(actionText, onAction, Modifier.width(200.dp))
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) = MessageState(
    icon = Icons.Outlined.WifiOff,
    title = "Не вдалося завантажити",
    text = "$message. Збереженої копії немає.",
    actionText = "Повторити",
    onAction = onRetry,
    iconTint = Color(0xFFF87171),
)

@Composable
fun OfflineBanner(message: String) {
    val amber = Color(0xFFFBBF24)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(amber.copy(alpha = 0.12f))
            .border(1.dp, amber.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.CloudOff, null, tint = amber, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text("Офлайн: $message. Показано збережені дані.", color = amber, fontSize = 12.5.sp)
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Abit.colors
    Box(
        modifier
            .height(48.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(Brush.linearGradient(listOf(c.primary, c.primaryEnd)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Abit.colors
    val shape = RoundedCornerShape(15.dp)
    Box(
        modifier
            .height(48.dp)
            .clip(shape)
            .background(c.glassStrong)
            .border(1.dp, c.outline, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = c.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
}

/** Скелетон: пульсуючі скляні рядки на місці майбутнього списку. */
@Composable
fun SkeletonList(rows: Int = 5) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "alpha",
    )
    Column(Modifier.alpha(alpha), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(rows) {
            GlassCard(Modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        .fillMaxWidth(0.7f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Abit.colors.glassStrong)
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth(0.4f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Abit.colors.glassStrong)
                )
            }
        }
    }
}
