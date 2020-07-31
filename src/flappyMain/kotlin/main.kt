package flappy

import cnames.structs.SDL_Renderer
import kotlinx.cinterop.*
import sdl.*

const val FPS = 60
const val INTERVALO_CANOS = 240

data class Cano(
        var x: Int = 0,
        var y: Int = 0
)

@ExperimentalUnsignedTypes
fun main() {
    SDL_Init(SDL_INIT_VIDEO)
    IMG_Init(IMG_INIT_JPG.toInt() or IMG_INIT_PNG.toInt())

    val janela = SDL_CreateWindow("Super Projeto", SDL_WINDOWPOS_UNDEFINED.toInt(), SDL_WINDOWPOS_UNDEFINED.toInt(),
            640, 480, SDL_WINDOW_SHOWN)
    val renderer = SDL_CreateRenderer(janela, -1, SDL_RENDERER_SOFTWARE)
    val tela = SDL_GetWindowSurface(janela)
    val fundo = IMG_Load("bg.jpg")

    val arena = Arena()

    var opcaoMenu = 1
    var sair = false
    val event = arena.alloc<SDL_Event>()
    while (!sair) {
        while (SDL_PollEvent(event.ptr.reinterpret()) != 0) {
            when (event.type) {
                SDL_QUIT -> sair = true
                SDL_KEYDOWN -> {
                    when (event.key.keysym.sym.toUInt()) {
                        SDLK_ESCAPE -> sair = true
                        SDLK_DOWN -> {
                            opcaoMenu++
                            if (opcaoMenu > 3) {
                                opcaoMenu = 1
                            }
                        }
                        SDLK_UP -> {
                            opcaoMenu--
                            if (opcaoMenu < 1) {
                                opcaoMenu = 3
                            }
                        }
                        SDLK_RETURN -> {
                            if (opcaoMenu == 3) {
                                sair = true
                            } else if (opcaoMenu == 1) {
                                // Mostra jogo
                                mostraJogo(renderer)
                            }
                        }
                    }
                }
            }
        }

        SDL_BlitSurface?.invoke(fundo, null, tela, null)
        montaMenu(tela, opcaoMenu)
        SDL_UpdateWindowSurface(janela)
    }

    arena.clear()
    SDL_FreeSurface(fundo)
    SDL_DestroyRenderer(renderer)
    SDL_DestroyWindow(janela)
    IMG_Quit()
    SDL_Quit()
}

@ExperimentalUnsignedTypes
fun mostraJogo(renderer: CPointer<SDL_Renderer>?) {
    val canos = listOf(Cano(), Cano(), Cano(), Cano())

    var y = 240
    var gameOver = false
    var sair = false
    var aceleracao = 0f
    val raio = 20

    canos[0].apply {
        this.x = 750
        this.y = geraAlturaCano()
    }

    canos[1].apply {
        this.x = 750 + INTERVALO_CANOS
        this.y = geraAlturaCano()
    }

    canos[2].apply {
        this.x = 750 + INTERVALO_CANOS * 2
        this.y = geraAlturaCano()
    }

    canos[3].apply {
        this.x = 750 + INTERVALO_CANOS * 3
        this.y = geraAlturaCano()
    }

    val arena = Arena()
    val event = arena.alloc<SDL_Event>()
    while (!sair) {
        val tempoInicial = SDL_GetTicks()

        SDL_SetRenderDrawColor(renderer, 0x00, 0x00, 0x00, 0xff)
        SDL_RenderClear(renderer)

        canos.forEach { desenhaCano(renderer, it) }

        filledCircleColor(renderer, 70, y.toShort(), raio.toShort(), 0xff0000ff)

        y = atualizarPosicao(aceleracao, y).toInt()
        aceleracao -= 0.25f

        if (y > 480 - raio) {
            y = 480 - raio
            aceleracao = 0f
        }

        if (y < raio) {
            y = raio
            aceleracao = 0f
        }

        while (SDL_PollEvent(event.ptr.reinterpret()) != 0) {
            when (event.type) {
                SDL_KEYDOWN -> when (event.key.keysym.sym.toUInt()) {
                    SDLK_ESCAPE -> sair = true
                }
                SDL_MOUSEBUTTONDOWN -> {
                    if (!gameOver) aceleracao = 5f
                }
            }
        }

        if (!gameOver) {
            for (cano in canos) {
                gameOver = verificaColisao(70, y, cano)

                if (gameOver) break
            }

            canos.forEach {
                atualizarCanos(it)
            }
        }

        val tempoFinal = SDL_GetTicks() - tempoInicial
        if (tempoFinal.toDouble() < 1.0 / FPS) {
            SDL_Delay((1000 / FPS - tempoFinal.toInt()).toUInt())
        }

        SDL_RenderPresent(renderer)
    }

    arena.clear()
}

fun atualizarPosicao(aceleracao: Float, posicao: Int) = posicao - aceleracao

@ExperimentalUnsignedTypes
fun desenhaCano(renderer: CPointer<SDL_Renderer>?, cano: Cano) {
    boxColor(renderer, cano.x.toShort(), 0, (cano.x + 80).toShort(), cano.y.toShort(), 0xff00ffff)

    boxColor(renderer, cano.x.toShort(), (cano.y + 120).toShort(), (cano.x + 80).toShort(), 480, 0xff00ffff)
}

fun geraAlturaCano() = (0..350).random() + 10

fun atualizarCanos(cano: Cano) {
    cano.x = cano.x - 2

    if (cano.x < -80) {
        cano.x = 640 + INTERVALO_CANOS
        cano.y = geraAlturaCano()
    }
}

fun verificaColisao(x: Int, y: Int, cano: Cano): Boolean {
    val raio = 20
    val distanciaCanos = 120
    val larguraCano = 80

    if (x - raio > cano.x + larguraCano) return false

    if (x + raio < cano.x) return false

    if (y - raio > cano.y &&
            y + raio < cano.y + distanciaCanos) return false

    return true
}

fun montaMenu(tela: CPointer<SDL_Surface>?, opcaoMenu: Int) {
    val config = IMG_Load("config.png")
    val configPressed = IMG_Load("config_pressed.png")
    val iniciar = IMG_Load("iniciar.png")
    val iniciarPressed = IMG_Load("iniciar_pressed.png")
    val sair = IMG_Load("sair.png")
    val sairPressed = IMG_Load("sair_pressed.png")

    memScoped {
        val dest = alloc<SDL_Rect>()
        if (opcaoMenu == 1) {
            dest.apply {
                x = (tela?.get(0)?.w ?: 0) / 2 - (iniciarPressed?.get(0)?.w ?: 0) / 2
                y = 15
                w = iniciarPressed?.get(0)?.w ?: 0
                h = iniciarPressed?.get(0)?.h ?: 0
            }
            SDL_BlitSurface?.invoke(iniciarPressed, null, tela, dest.ptr.reinterpret())
        } else {
            dest.apply {
                x = (tela?.get(0)?.w ?: 0) / 2 - (iniciar?.get(0)?.w ?: 0) / 2
                y = 15
                w = iniciar?.get(0)?.w ?: 0
                h = iniciar?.get(0)?.h ?: 0
            }
            SDL_BlitSurface?.invoke(iniciar, null, tela, dest.ptr.reinterpret())
        }

        if (opcaoMenu == 2) {
            dest.apply {
                x = (tela?.get(0)?.w ?: 0) / 2 - (configPressed?.get(0)?.w ?: 0) / 2
                y = (tela?.get(0)?.h ?: 0) / 2 - (configPressed?.get(0)?.h ?: 0) / 2
                w = configPressed?.get(0)?.w ?: 0
                h = configPressed?.get(0)?.h ?: 0
            }
            SDL_BlitSurface?.invoke(configPressed, null, tela, dest.ptr.reinterpret())
        } else {
            dest.apply {
                x = (tela?.get(0)?.w ?: 0) / 2 - (config?.get(0)?.w ?: 0) / 2
                y = (tela?.get(0)?.h ?: 0) / 2 - (config?.get(0)?.h ?: 0) / 2
                w = config?.get(0)?.w ?: 0
                h = config?.get(0)?.h ?: 0
            }
            SDL_BlitSurface?.invoke(config, null, tela, dest.ptr.reinterpret())
        }

        if (opcaoMenu == 3) {
            dest.apply {
                x = (tela?.get(0)?.w ?: 0) / 2 - (sairPressed?.get(0)?.w ?: 0) / 2
                y = (tela?.get(0)?.h ?: 0) - 15 - (sairPressed?.get(0)?.h ?: 0)
                w = config?.get(0)?.w ?: 0
                h = config?.get(0)?.h ?: 0
            }
            SDL_BlitSurface?.invoke(sairPressed, null, tela, dest.ptr.reinterpret())
        } else {
            dest.apply {
                x = (tela?.get(0)?.w ?: 0) / 2 - (sair?.get(0)?.w ?: 0) / 2
                y = (tela?.get(0)?.h ?: 0) - 15 - (sair?.get(0)?.h ?: 0)
                w = config?.get(0)?.w ?: 0
                h = config?.get(0)?.h ?: 0
            }
            SDL_BlitSurface?.invoke(sair, null, tela, dest.ptr.reinterpret())
        }
    }

    SDL_FreeSurface(config)
    SDL_FreeSurface(configPressed)
    SDL_FreeSurface(iniciar)
    SDL_FreeSurface(iniciarPressed)
    SDL_FreeSurface(sair)
    SDL_FreeSurface(sairPressed)
}
