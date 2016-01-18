if (0) {
  x <- 1:9
  nwin <- 2
  n <- nwin + 1
  m <- NULL
  for (i in 1:n) m <- rbind(m, rep(i, n))
  m <- rbind(m, (n+1):(2*n))
  layout(m, widths=1, heights=1)
  for (i in 1:max(m)) plot(x, main=i)
  stop()
}

# source ('~/util/R/sim-eeg.R')
# simulate EEG spectrum by superposing number of sinusoids with variable amplitudes and freq.
# The purpose is to be able to create EEG-like signal with known characteristics
# so the functions for EEG spectral analysis, etc. can be easily tested.

window_amp <- function(tott,         # total time in seconds
                       wint,         # window time in seconds
                       sampling_frq,
                       amp=c(3,5,7),
                       nsmooth=NULL, # number of smoothing points for transition; if NULL no smoothing is done
  		       units=list(time='s', amplitude='?'),
                       verbose=0,
                       debug=0) {
   #
   # create amplitude vectors such that in each window we have repeating values from amp, e.g.
   # with wint = 1, tott = 2*wint, sampling_frq = 3 and amp = c(3,5,7) we could have
   # am1 = 333555
   # am2 = 777333
   # am3 = 555777
   #
   if (wint > tott) {
      cat('WARNING: window_amp: window time > total time; wint=', wint, 'tott=', tott, '\n')
      wint <- tott
      cat('WARNING: window_amp: reset:  wint=', wint, 'tott=', tott, '\n')
   }
   nw <- as.integer(tott/wint) # number of windows
   if (nw * wint != tott) {
      cat('WARNING: window_amp: reset:  tott reset; old tott=', tott, '\n')
      tott <- nw*wint
      cat('WARNING: window_amp: new tott=', tott, 'windt=', wint, '\n')
   }
   np <- tott * sampling_frq # total number of points
   npw <- rep(np/nw, nw) # number of points per window; currenty is the same but may be variable in the future...
   v <- list()
   for (j in 1:length(amp)) v[[j]] <- rep(NA, np)
   if (debug) cat('DEBUG: window_amp: length(v[[1]])=', length(v[[1]]), '\n')
   start <- 1
   for (i in 1:nw) {
      end <- start + npw[i] - 1
      am <- sample(amp)
      for (j in 1:length(amp)) {
         v[[j]][start:end] <- rep(am[j], npw[i])
      }
      start <- end + 1
   }
   if (!is.null(nsmooth)) {
      for (j in 1:length(v)) {
         v[[j]] <- smooth(v[[j]], nsmooth)
      }
   }
   if (verbose) {
     plt.amp(amp, v, tott, sampling_frq, units)
   }
   return (v) 
}

plt.fft.power <- function(t, xt) {
     dt <- max(t) - min(t)
     f <- fft(xt)/length(xt)
     p <- Mod(f[1:(length(f)/2)])
     f <- (0:(length(p)-1))/dt # divide by time interval to get frq units right
     plot(f, p, type='l', xlab=sprintf('frequency [Hz]'), ylab=sprintf('power'), xlim=c(0,1.1*max(frq)))
}

plt.amp <- function(amp,          # input amplitudes values
                    am,           # list with vectors of precomputed amplitudes for each time point
                    tott,         # total simulation time
                    sampling_frq,
                    units,        # list with units for time and amplitudes
                    title=NULL,
                    debug=0) {
     plot(1, type='n', xlim=c(0, tott), ylim=range(amp), main=title,
          xlab=sprintf('time [%s]', units$time), ylab=sprintf('amplitudes [%s]', units$amplitude))
     col <- rainbow(length(am))
     dt <- 1/sampling_frq
     t <- seq(dt, tott, dt)
     if (debug) {
        cat('DEBUG: plt.amp: length(t)=', length(t), 'sampling_frq=', sampling_frq, 'dt=', dt, '\n')
     }
     for (i in 1:length(am)) {
        if (debug) cat('DEBUG: plt.amp: i, length(am[[i]])=', i, length(am[[i]]), '\n')
        lines(t, am[[i]], col=col[i], lwd=2)
     }
}

smooth <- function(x, n) {
   library(signal)
   y <- stats::filter(x, signal::triang(n))/sum(triang(n))
   # y <- stats::filter(x, signal::gausswin(n))/sum(gausswin(n))
   w <- which(is.na(y))
   y <- as.numeric(y)
   y[w] <- x[w]
   return (y)
}


simEEG <- function(tott,                # total simulation time in seconds
                   wint,                # window time in secods
                   amp = c(3,5,7),      # signal ampltitudes
                   frq = 2^(0:2),       # signal frequencies
                   ph = rep(0,3),       # phase shifts
                   sampling_frq = 128,  # Hz i.e. (1/s)
                   smooth_frac = NULL,  # roughly fraction of window length to smooth transition between windows 
                   units = list(time='s', amplitude='?'),
                   period = 1,          # TBD: is this needed?
                   fn = NULL,           # optional file name for plot
                   verbose = 0,
                   debug = 0) {
   if (verbose) {
      cat('simEEG: amp=', amp, 'frq=', frq, 'ph=', ph, '\n')
      cat('simEEG: sampling_frq=', sampling_frq, 'smooth_frac=', smooth_frac, '\n')
   }
   ns <- na <- length(amp); nf <- length(frq); nh <- length(ph)
   if (debug) {
      cat('ns =', ns, 'na =', na, 'nf =', nf, 'nh =', nh, '\n')
   }
   if (ns != nf | ns != nh) {
      cat('WARNING: amplitudes: lengths of amp, frq ph not the same: na, nf, nh =', na, nf, nh, '\n')
      ns <- max(1, min(ns, nf, nh))
      cat('WARNING: amplitudes: the smallest value will be used: ns =', ns, '\n')
   }
   np <- tott * sampling_frq # total nuber of points
   nw <- wint * sampling_frq # number of points per window
   if ((floor(np/nw)*nw != np)) {
      nw <- 2^floor(log2(nw))
      np <- floor(np/nw)*nw
      cat('WARNING: amplitudes: nw does not divide np evenly; reset new values: np, nw =', np, nw, '\n')
   }
   v <- rep(0, np)
   t <- 1:np / sampling_frq
   if (debug) {
      cat('DEBUG: simEGG: np =', np, 'length(t)=', length(t), 'tott=', tott, 'max(t)=', max(t), '\n')
   }
   if (!is.null(smooth_frac)) {
     nsmooth <- 2^floor(log2(wint*sampling_frq*smooth_frac))+1
   } else {
     nsmooth <- NULL # no amplitude smoothing between windows
   }
   wam <- window_amp(tott, wint, sampling_frq, amp, nsmooth, verbose=verbose, debug=0)
   for (i in 1:ns) { # sum over signal components
     w <- (frq[i]*t + ph[i])*(2*pi/period)
     # v <- v + amp[i]*sin(w)
     v <- v + wam[[i]]*sin(w)
   }
   if (verbose) {
     tt <- paste(round(frq,2), collapse=', ')
     title <- paste('frq=', tt, '[Hz]', collapse='') 
     if (!is.null(fn)) png(fn)
     nwin <- tott / wint # number of windows (total time / window time)
     n <- 2*nwin - 1
     m <- NULL
     for (i in 1:2) m <- rbind(m, rep(i, n))
     m <- rbind(m, 2 + 1:n)
     cat('m=\n'); print (m)
     layout(m, widths=1, heights=1)
     plt.amp(amp, wam, tott, sampling_frq, units, title, debug=0)
     plot(t, v, type='l', xlab=sprintf('time[%s]', units$time), ylab=sprintf('signal[%s]', units$amplitude))
     # plt.fft.power(t, v) # for the whole signal
     #
     # now make fft & plots for overlapping windows
     #
     no <- 2*nwin - 1     # number of overlapping windows (by half length)
     np <- length(v)/nwin # number of points in one window
     # nwin <- 2; np <- 4
     starts <- (0:(2*nwin-2))*(np/2)
     # points in i-th window: starts[i] + 1:np
     for (i0 in starts) {
        i <- i0 + 1:np
        plt.fft.power(t[i], v[i]) 
     }
     cat('length(v)=', length(v), 'min(t), max(t)=', min(t), max(t), '\n')
     cat('nwin, no, np =', nwin, no, np, 'starts=', starts, '\n')
     if (!is.null(fn)) dev.off(dev.cur())
   }
   return(v)
} # end_simEGG

get.eegFrq <- function() {
  # we can use these at some point for more realistic simulations
  # EEG frequency ranges [Hz]
  # from http://www.brainmaster.com/generalinfo/eegbands/eegbands.html
  eegFrq <- list(delta=c(0.1, 3),
        	 theta=c(4, 7),
                 alpha=c(8,12),
                 betaLow=c(12,15),
                 betaMid=c(15,18),
                 betaHigh=c(19,99), # above 18 
                 gamma=c(39,41))
  return (eegFrq)
}

amp <- 1.5^(0:6)
amp <- rev(amp)
fbase <- sqrt(3)
frq <- fbase^(0:(length(amp)-1))
# frq <- round(1 + 4*runif(length(amp)),2)
ph <- rep(0,length(amp))
sampling_frq <- 256*1
wint <- 32
smooth_frac <- 1/16
nsmooth <- 2^floor(log2(wint*sampling_frq*smooth_frac))+1
tott <- 3*wint

set.seed(4)
# wam <- window_amp(tott, wint, sampling_frq, amp, nsmooth, verbose=1)
fn.plt <- 'simEEG.png'
# fn.plt <- NULL
v <- simEEG(tott, wint, amp, frq, ph, sampling_frq, smooth_frac=smooth_frac, fn=fn.plt, verbose=1, debug=0)
# v <- simEEG(tott, wint, amp, frq, ph, sampling_frq, smooth_frac=smooth_frac, verbose=1, fn='sim-eeg.png', debug=0)
# plot(v, type='l')

# source ('~/util/R/sim-eeg.R')

stop('...')
